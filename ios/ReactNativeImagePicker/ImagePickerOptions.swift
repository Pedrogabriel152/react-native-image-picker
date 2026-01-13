import Foundation
import UIKit

struct ImagePickerOptions {
  enum MediaType {
    case photo
    case video
    case any
  }

  let selectionLimit: Int
  let mediaType: MediaType
  let maxWidth: CGFloat
  let maxHeight: CGFloat
  let quality: CGFloat
  let includeBase64: Bool
  let includeExtra: Bool
  let saveToPhotos: Bool
  let durationLimit: TimeInterval
  let videoQuality: UIImagePickerController.QualityType
  let useFrontCamera: Bool
  let restrictMimeTypes: [String]

  init(_ options: [String: Any]?) {
    let selectionLimit = (options?["selectionLimit"] as? Int) ?? 1
    self.selectionLimit = selectionLimit

    let mediaTypeString = (options?["mediaType"] as? String) ?? "photo"
    switch mediaTypeString.lowercased() {
    case "video":
      self.mediaType = .video
    case "any", "mixed":
      self.mediaType = .any
    default:
      self.mediaType = .photo
    }

    self.maxWidth = (options?["maxWidth"] as? CGFloat) ?? 0
    self.maxHeight = (options?["maxHeight"] as? CGFloat) ?? 0

    let quality = (options?["quality"] as? CGFloat) ?? 1.0
    self.quality = max(0, min(quality, 1.0))

    self.includeBase64 = (options?["includeBase64"] as? Bool) ?? false
    self.includeExtra = (options?["includeExtra"] as? Bool) ?? false
    self.saveToPhotos = (options?["saveToPhotos"] as? Bool) ?? false

    self.durationLimit = (options?["durationLimit"] as? TimeInterval) ?? 0

    let videoQualityString = (options?["videoQuality"] as? String) ?? "low"
    switch videoQualityString.lowercased() {
    case "high":
      self.videoQuality = .typeHigh
    case "medium":
      self.videoQuality = .typeMedium
    default:
      self.videoQuality = .typeLow
    }

    self.useFrontCamera = (options?["cameraType"] as? String) == "front"

    let restrictTypes = (options?["restrictMimeTypes"] as? [String]) ?? []
    self.restrictMimeTypes = restrictTypes
  }
}
