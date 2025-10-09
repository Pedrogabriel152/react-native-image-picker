import ReactNativeImagePicker, {
  type LaunchOptions,
} from './NativeReactNativeImagePicker';

export function multiply(a: number, b: number): number {
  return ReactNativeImagePicker.multiply(a, b);
}

/**
 * Abre a galeria para selecionar imagens/vídeos
 * @param options Opções de seleção
 * @returns Promise que resolve com os resultados (incluindo EXIF)
 */
export function launchImageLibrary(options: LaunchOptions = {}): Promise<any> {
  return ReactNativeImagePicker.launchImageLibrary(options);
}

export function launchCamera(options: LaunchOptions = {}): Promise<any> {
  return ReactNativeImagePicker.launchCamera(options);
}
