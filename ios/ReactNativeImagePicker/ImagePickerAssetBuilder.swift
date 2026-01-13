import Foundation
import UIKit
import AVFoundation

struct ImagePickerAssetContext {
  let id: String?
  let creationDate: Date?
  let originalURL: URL?
}

struct ImagePickerAssetBuilder {
  static func buildImageAsset(url: URL, options: ImagePickerOptions, context: ImagePickerAssetContext?) -> [String: Any] {
    let image = UIImage(contentsOfFile: url.path)
    let size = image?.size ?? .zero
    let fileName = context?.originalURL?.lastPathComponent ?? url.lastPathComponent

    var asset: [String: Any] = [
      "uri": url.absoluteString,
      "fileSize": ImagePickerUtils.fileSize(for: url),
      "fileName": fileName,
      "width": Int(size.width),
      "height": Int(size.height),
      "type": ImagePickerUtils.mimeType(for: url),
      "originalPath": context?.originalURL?.path ?? url.path
    ]

    if options.includeBase64 {
      if let data = try? Data(contentsOf: url) {
        asset["base64"] = data.base64EncodedString()
      }
    }

    if options.includeExtra {
      asset["timestamp"] = ImagePickerUtils.isoTimestamp(from: context?.creationDate)
      asset["id"] = context?.id ?? fileName
    }

    return asset
  }

  static func buildVideoAsset(url: URL, options: ImagePickerOptions, context: ImagePickerAssetContext?) -> [String: Any] {
    let asset = AVAsset(url: url)
    let durationSeconds = Int(CMTimeGetSeconds(asset.duration).rounded())

    var width = 0
    var height = 0
    var bitrate = 0

    if let track = asset.tracks(withMediaType: .video).first {
      let size = track.naturalSize.applying(track.preferredTransform)
      width = Int(abs(size.width))
      height = Int(abs(size.height))
      bitrate = Int(track.estimatedDataRate)
    }

    let fileName = context?.originalURL?.lastPathComponent ?? url.lastPathComponent

    var result: [String: Any] = [
      "uri": url.absoluteString,
      "fileSize": ImagePickerUtils.fileSize(for: url),
      "duration": durationSeconds,
      "bitrate": bitrate,
      "fileName": fileName,
      "type": ImagePickerUtils.mimeType(for: url),
      "width": width,
      "height": height,
      "originalPath": context?.originalURL?.path ?? url.path
    ]

    if options.includeExtra {
      result["timestamp"] = ImagePickerUtils.isoTimestamp(from: context?.creationDate)
      result["id"] = context?.id ?? fileName
    }

    return result
  }
}
