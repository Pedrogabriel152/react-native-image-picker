import ReactNativeImagePicker, {
  type LaunchOptions,
} from './NativeReactNativeImagePicker';

export type { LaunchOptions } from './NativeReactNativeImagePicker';

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

export type PendingResult = {
  assets: any[];
  /** O mesmo `tag` passado em launchCamera, ou null. */
  tag: string | null;
  recovered: true;
};

/**
 * Android: recupera a captura da câmera perdida quando o sistema mata o app em segundo plano
 * (comum em aparelhos Samsung). Chame ao montar a tela que abriu a câmera.
 * Resolve null quando não há nada pendente (sempre null no iOS).
 */
export function getPendingResult(): Promise<PendingResult | null> {
  return ReactNativeImagePicker.getPendingResult() as Promise<PendingResult | null>;
}
