import {
  NativeModules,
  TurboModuleRegistry,
  type TurboModule,
} from 'react-native';

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
  includeBase64?: boolean;
  includeExtra?: boolean;
  saveToPhotos?: boolean;
  durationLimit?: number;
  videoQuality?: 'low' | 'medium' | 'high';
  cameraType?: 'back' | 'front';
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

type LegacySpec = {
  multiply(a: number, b: number): number;
  launchImageLibrary(options: LaunchOptions): Promise<any>;
  launchCamera(options: LaunchOptions): Promise<any>;
};

const turboModule = TurboModuleRegistry.get<Spec>('ReactNativeImagePicker');
const legacyModule = NativeModules.ReactNativeImagePicker as
  | LegacySpec
  | undefined;

const moduleProxy = (turboModule ?? legacyModule) as
  | Spec
  | LegacySpec
  | undefined;

if (!moduleProxy) {
  throw new Error(
    'ReactNativeImagePicker native module not found. Ensure pods/gradle are installed and the app was rebuilt.'
  );
}

export default moduleProxy as Spec;
