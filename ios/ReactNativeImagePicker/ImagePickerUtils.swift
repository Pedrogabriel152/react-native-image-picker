import Foundation
import UIKit
import Photos
import AVFoundation
import MobileCoreServices
import UniformTypeIdentifiers
import React

enum ImagePickerError: String {
  case cameraUnavailable = "camera_unavailable"
  case permission = "permission"
  case others = "others"
}

struct ImagePickerUtils {
  static let filePrefix = "rn_image_picker_lib_temp_"

  static func presentedViewController() -> UIViewController? {
    return RCTPresentedViewController()
  }

  static func temporaryFileURL(fileExtension: String) -> URL {
    let fileName = "\(filePrefix)\(UUID().uuidString).\(fileExtension)"
    return FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
  }

  static func fileSize(for url: URL) -> Double {
    do {
      let attributes = try FileManager.default.attributesOfItem(atPath: url.path)
      return (attributes[.size] as? NSNumber)?.doubleValue ?? 0
    } catch {
      return 0
    }
  }

  static func mimeType(for url: URL) -> String {
    let fileExtension = url.pathExtension.lowercased()
    if #available(iOS 14.0, *) {
      if let type = UTType(filenameExtension: fileExtension), let mime = type.preferredMIMEType {
        return mime
      }
    }

    if let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, fileExtension as CFString, nil)?.takeRetainedValue(),
       let mime = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType)?.takeRetainedValue() as String? {
      return mime
    }

    return "application/octet-stream"
  }

  static func isoTimestamp(from date: Date?) -> String? {
    guard let date else { return nil }
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return formatter.string(from: date)
  }

  static func scaledSize(for original: CGSize, maxWidth: CGFloat, maxHeight: CGFloat) -> CGSize {
    guard maxWidth > 0 || maxHeight > 0 else { return original }

    let targetMaxWidth = maxWidth > 0 ? maxWidth : original.width
    let targetMaxHeight = maxHeight > 0 ? maxHeight : original.height

    let widthRatio = targetMaxWidth / original.width
    let heightRatio = targetMaxHeight / original.height
    let scale = min(widthRatio, heightRatio, 1)

    return CGSize(width: original.width * scale, height: original.height * scale)
  }

  static func resizeImage(_ image: UIImage, maxWidth: CGFloat, maxHeight: CGFloat) -> UIImage {
    let targetSize = scaledSize(for: image.size, maxWidth: maxWidth, maxHeight: maxHeight)
    guard targetSize != image.size else { return image }

    let renderer = UIGraphicsImageRenderer(size: targetSize)
    return renderer.image { _ in
      image.draw(in: CGRect(origin: .zero, size: targetSize))
    }
  }
}
