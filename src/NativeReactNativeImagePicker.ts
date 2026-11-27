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
  includeBase64?: boolean;
  includeExtra?: boolean;
  saveToPhotos?: boolean;
  cameraType?: 'back' | 'front';
  /**
   * Valor opaco salvo junto com a captura da câmera (Android) e devolvido por
   * getPendingResult(), para identificar a quem a foto pertence após o app ser reiniciado.
   */
  tag?: string;
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
  /**
   * Android: recupera a foto/vídeo da última launchCamera cujo resultado foi perdido porque
   * o sistema encerrou o processo do app enquanto a câmera estava aberta. Resolve null se não houver.
   */
  getPendingResult(): Promise<Object | null>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeImagePicker');
