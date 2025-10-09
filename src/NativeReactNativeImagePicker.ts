import { TurboModuleRegistry, type TurboModule } from 'react-native';

export type PickerResult = {
  uri: string;
  filePath: string;
  exif: Record<string, string>;
};

export type LaunchOptions = {
  selectionLimit?: number;
  mediaType?: 'photo' | 'video' | 'any';
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
  restrictMimeTypes?: string[];
};

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;
  /**
   * Abre a galeria do dispositivo para selecionar imagens/vídeos
   * @param options Configurações de seleção
   * @param callback Callback (erro, resultados)
   */
  launchImageLibrary(options: LaunchOptions): Promise<any>;
  launchCamera(options: LaunchOptions): Promise<any>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeImagePicker');
