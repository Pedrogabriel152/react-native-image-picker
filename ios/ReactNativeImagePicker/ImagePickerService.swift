import Foundation
import UIKit
import Photos
import PhotosUI
import MobileCoreServices
import UniformTypeIdentifiers
import React

@objcMembers
public final class RNImagePickerService: NSObject {
  private enum RequestType {
    case camera
    case library
  }

  private var resolve: RCTPromiseResolveBlock?
  private var reject: RCTPromiseRejectBlock?
  private var options = ImagePickerOptions(nil)
  private var activeRequest: RequestType?

  public func launchImageLibraryWithOptions(_ options: NSDictionary,
                                            resolve: @escaping RCTPromiseResolveBlock,
                                            reject: @escaping RCTPromiseRejectBlock) {
    self.resolve = resolve
    self.reject = reject
    self.options = ImagePickerOptions(options as? [String: Any])
    self.activeRequest = .library

    DispatchQueue.main.async {
      if #available(iOS 14.0, *) {
        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.selectionLimit = self.options.selectionLimit == 0 ? 0 : self.options.selectionLimit
        switch self.options.mediaType {
        case .photo:
          config.filter = .images
        case .video:
          config.filter = .videos
        case .any:
          config.filter = .any(of: [.images, .videos])
        }

        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        self.present(picker)
      } else {
        let picker = UIImagePickerController()
        picker.sourceType = .photoLibrary
        picker.mediaTypes = self.mediaTypes(for: self.options.mediaType)
        picker.delegate = self
        self.present(picker)
      }
    }
  }

  public func launchCameraWithOptions(_ options: NSDictionary,
                                      resolve: @escaping RCTPromiseResolveBlock,
                                      reject: @escaping RCTPromiseRejectBlock) {
    self.resolve = resolve
    self.reject = reject
    self.options = ImagePickerOptions(options as? [String: Any])
    self.activeRequest = .camera

    DispatchQueue.main.async {
#if targetEnvironment(simulator)
      // Simulator camera capture is unreliable; fallback to library.
      self.activeRequest = .library
      let picker = UIImagePickerController()
      picker.sourceType = .photoLibrary
      picker.mediaTypes = self.mediaTypes(for: self.options.mediaType)
      picker.delegate = self
      self.present(picker)
      return
#endif
      guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
        self.reject?(ImagePickerError.cameraUnavailable.rawValue, "Camera not available", nil)
        self.cleanup()
        return
      }

      self.ensurePhotoLibraryPermissionIfNeeded { authorized in
        if !authorized {
          self.reject?(ImagePickerError.permission.rawValue, "Access denied", nil)
          self.cleanup()
          return
        }

        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.mediaTypes = self.mediaTypes(for: self.options.mediaType)
        picker.videoQuality = self.options.videoQuality
        if self.options.durationLimit > 0 {
          picker.videoMaximumDuration = self.options.durationLimit
        }
        if UIImagePickerController.isCameraDeviceAvailable(.front), self.options.useFrontCamera {
          picker.cameraDevice = .front
        }
        picker.delegate = self
        self.present(picker)
      }
    }
  }

  private func present(_ controller: UIViewController) {
    guard let presenter = ImagePickerUtils.presentedViewController() else {
      reject?(ImagePickerError.others.rawValue, "No view controller to present", nil)
      cleanup()
      return
    }
    presenter.present(controller, animated: true)
  }

  private func mediaTypes(for mediaType: ImagePickerOptions.MediaType) -> [String] {
    let imageType: String
    let movieType: String

    if #available(iOS 14.0, *) {
      imageType = UTType.image.identifier
      movieType = UTType.movie.identifier
    } else {
      imageType = kUTTypeImage as String
      movieType = kUTTypeMovie as String
    }

    switch mediaType {
    case .photo:
      return [imageType]
    case .video:
      return [movieType]
    case .any:
      return [imageType, movieType]
    }
  }

  private func ensurePhotoLibraryPermissionIfNeeded(completion: @escaping (Bool) -> Void) {
    guard options.saveToPhotos else {
      completion(true)
      return
    }

    if #available(iOS 14.0, *) {
      let status = PHPhotoLibrary.authorizationStatus(for: .addOnly)
      handlePhotoAuthorization(status: status, completion: completion, requestAddOnly: true)
    } else {
      let status = PHPhotoLibrary.authorizationStatus()
      handlePhotoAuthorization(status: status, completion: completion, requestAddOnly: false)
    }
  }

  private func handlePhotoAuthorization(status: PHAuthorizationStatus,
                                        completion: @escaping (Bool) -> Void,
                                        requestAddOnly: Bool) {
    switch status {
    case .authorized, .limited:
      completion(true)
    case .notDetermined:
      if #available(iOS 14.0, *), requestAddOnly {
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { newStatus in
          completion(newStatus == .authorized || newStatus == .limited)
        }
      } else {
        PHPhotoLibrary.requestAuthorization { newStatus in
          completion(newStatus == .authorized)
        }
      }
    @unknown default:
      completion(false)
    }
  }

  private func resolveCancel() {
    resolve?(["didCancel": true])
    cleanup()
  }

  private func resolveAssets(_ assets: [[String: Any]]) {
    resolve?(["assets": assets])
    cleanup()
  }

  private func cleanup() {
    resolve = nil
    reject = nil
    activeRequest = nil
  }

  private func saveToPhotosIfNeeded(fileURL: URL, isVideo: Bool, completion: @escaping (Bool) -> Void) {
    guard options.saveToPhotos, activeRequest == .camera else {
      completion(true)
      return
    }

    PHPhotoLibrary.shared().performChanges({
      if isVideo {
        PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: fileURL)
      } else {
        if let data = try? Data(contentsOf: fileURL), let image = UIImage(data: data) {
          PHAssetChangeRequest.creationRequestForAsset(from: image)
        }
      }
    }) { success, _ in
      completion(success)
    }
  }

  private func copyToTemp(from url: URL) -> URL? {
    let fileExtension = url.pathExtension.isEmpty ? "jpg" : url.pathExtension
    let destination = ImagePickerUtils.temporaryFileURL(fileExtension: fileExtension)
    do {
      try FileManager.default.copyItem(at: url, to: destination)
      return destination
    } catch {
      return nil
    }
  }

  private func processImageFile(url: URL, context: ImagePickerAssetContext) -> URL? {
    let shouldResize = options.maxWidth > 0 || options.maxHeight > 0 || options.quality < 1
    let image = UIImage(contentsOfFile: url.path)

    guard let baseImage = image else {
      return copyToTemp(from: url) ?? url
    }

    if shouldResize {
      let resized = ImagePickerUtils.resizeImage(baseImage, maxWidth: options.maxWidth, maxHeight: options.maxHeight)
      guard let data = resized.jpegData(compressionQuality: options.quality) else {
        return copyToTemp(from: url) ?? url
      }
      let destination = ImagePickerUtils.temporaryFileURL(fileExtension: "jpg")
      do {
        try data.write(to: destination, options: [.atomic])
        return destination
      } catch {
        return copyToTemp(from: url) ?? url
      }
    }

    return copyToTemp(from: url) ?? url
  }

  private func handleFiles(_ files: [(URL, ImagePickerAssetContext, Bool)]) {
    DispatchQueue.global(qos: .userInitiated).async {
      var assets: [[String: Any]] = []

      for (url, context, isVideo) in files {
        let workingURL: URL
        if isVideo {
          workingURL = self.copyToTemp(from: url) ?? url
          assets.append(ImagePickerAssetBuilder.buildVideoAsset(url: workingURL, options: self.options, context: context))
        } else {
          let processed = self.processImageFile(url: url, context: context) ?? url
          assets.append(ImagePickerAssetBuilder.buildImageAsset(url: processed, options: self.options, context: context))
        }
      }

      DispatchQueue.main.async {
        self.resolveAssets(assets)
      }
    }
  }
}

@available(iOS 14.0, *)
extension RNImagePickerService: PHPickerViewControllerDelegate {
  public func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
    picker.dismiss(animated: true)

    if results.isEmpty {
      resolveCancel()
      return
    }

    let group = DispatchGroup()
    var fileResults: [(URL, ImagePickerAssetContext, Bool)] = []
    var loadError: Error?
    let lock = NSLock()

    for result in results {
      let provider = result.itemProvider
      let assetId = result.assetIdentifier
      let asset = assetId.flatMap { PHAsset.fetchAssets(withLocalIdentifiers: [$0], options: nil).firstObject }
      let context = ImagePickerAssetContext(id: assetId, creationDate: asset?.creationDate, originalURL: nil)

      if provider.hasItemConformingToTypeIdentifier(UTType.image.identifier) {
        group.enter()
        provider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { url, error in
          defer { group.leave() }
          if let error {
            lock.lock()
            loadError = error
            lock.unlock()
            return
          }
          guard let url else { return }
          lock.lock()
          fileResults.append((url, context, false))
          lock.unlock()
        }
      } else if provider.hasItemConformingToTypeIdentifier(UTType.movie.identifier) {
        group.enter()
        provider.loadFileRepresentation(forTypeIdentifier: UTType.movie.identifier) { url, error in
          defer { group.leave() }
          if let error {
            lock.lock()
            loadError = error
            lock.unlock()
            return
          }
          guard let url else { return }
          lock.lock()
          fileResults.append((url, context, true))
          lock.unlock()
        }
      }
    }

    group.notify(queue: .main) {
      if let error = loadError {
        self.reject?(ImagePickerError.others.rawValue, error.localizedDescription, error)
        self.cleanup()
        return
      }
      self.handleFiles(fileResults)
    }
  }
}

extension RNImagePickerService: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
  public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    picker.dismiss(animated: true) {
      self.resolveCancel()
    }
  }

  public func imagePickerController(_ picker: UIImagePickerController,
                                    didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
    picker.dismiss(animated: true)

    let mediaType = info[.mediaType] as? String
    let isVideo = mediaType == (kUTTypeMovie as String)

    if isVideo {
      guard let mediaURL = info[.mediaURL] as? URL else {
        reject?(ImagePickerError.others.rawValue, "Missing video URL", nil)
        cleanup()
        return
      }

      let asset = info[.phAsset] as? PHAsset
      let context = ImagePickerAssetContext(id: asset?.localIdentifier, creationDate: asset?.creationDate, originalURL: mediaURL)

      saveToPhotosIfNeeded(fileURL: mediaURL, isVideo: true) { success in
        if !success {
          self.reject?(ImagePickerError.others.rawValue, "Could not save video", nil)
          self.cleanup()
          return
        }
        self.handleFiles([(mediaURL, context, true)])
      }
    } else {
      if let imageURL = info[.imageURL] as? URL {
        let workingURL = copyToTemp(from: imageURL) ?? imageURL
        let asset = info[.phAsset] as? PHAsset
        let context = ImagePickerAssetContext(id: asset?.localIdentifier, creationDate: asset?.creationDate, originalURL: imageURL)

        saveToPhotosIfNeeded(fileURL: workingURL, isVideo: false) { success in
          if !success {
            self.reject?(ImagePickerError.others.rawValue, "Could not save image", nil)
            self.cleanup()
            return
          }
          self.handleFiles([(workingURL, context, false)])
        }
        return
      }

      guard let image = info[.originalImage] as? UIImage else {
        reject?(ImagePickerError.others.rawValue, "Missing image", nil)
        cleanup()
        return
      }

      let tempURL = ImagePickerUtils.temporaryFileURL(fileExtension: "jpg")
      let data = image.jpegData(compressionQuality: 1.0)
      try? data?.write(to: tempURL, options: [.atomic])

      let asset = info[.phAsset] as? PHAsset
      let context = ImagePickerAssetContext(id: asset?.localIdentifier, creationDate: asset?.creationDate, originalURL: tempURL)

      saveToPhotosIfNeeded(fileURL: tempURL, isVideo: false) { success in
        if !success {
          self.reject?(ImagePickerError.others.rawValue, "Could not save image", nil)
          self.cleanup()
          return
        }
        self.handleFiles([(tempURL, context, false)])
      }
    }
  }
}
