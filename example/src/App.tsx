import { Text, View, StyleSheet, TouchableOpacity, Image } from 'react-native';
import {
  launchImageLibrary,
  launchCamera,
} from '@pedro.gabriel/react-native-image-picker';
import { getLatLong } from '@pedro.gabriel/react-native-exif';
import { useState } from 'react';
import { Button } from './Button';

const MAX_SIZE = 2000;

import { PermissionsAndroid, Platform } from 'react-native';

async function requestPermissions() {
  if (Platform.OS === 'android') {
    const granted = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
      // Adicione a permissão de acesso à localização de mídia
      PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
      PermissionsAndroid.PERMISSIONS.ACCESS_MEDIA_LOCATION,
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES,
      PermissionsAndroid.PERMISSIONS.READ_MEDIA_VIDEO,
      PermissionsAndroid.PERMISSIONS.CAMERA,
    ]);

    if (granted) {
      return true;
    }

    return true;
  }
  return true;
}
requestPermissions();

export default function App() {
  const [value, setValue] = useState({
    latitude: 0,
    longitude: 0,
    altitude: 0,
    accuracy: 0,
    heading: 0,
    speed: 0,
    timestamp: 0,
    uri: '',
  });

  const selectFromCamera = async () => {
    console.log('selectFile');
    const result = await launchCamera({
      mediaType: 'photo',
      maxWidth: MAX_SIZE,
      maxHeight: MAX_SIZE,
      quality: 1,
      includeExtra: true,
    });
    console.log(result);
    if (result.didCancel) {
      return;
    }
    const firstAsset = result.assets?.[0];
    if (!firstAsset?.uri) {
      return;
    }

    const path =
      Platform.OS === 'ios' && firstAsset.id
        ? `ph://${firstAsset.id}`
        : firstAsset.uri;

    const loc = await getLatLong(path);
    console.log(result, loc);
    setValue({
      ...value,
      latitude: loc?.latitude ?? 0,
      longitude: loc?.longitude ?? 0,
      uri: firstAsset.uri,
    });
  };

  const selectFromLibrary = async () => {
    console.log('selectFile');
    const result = await launchImageLibrary({
      mediaType: 'photo',
      maxWidth: MAX_SIZE,
      maxHeight: MAX_SIZE,
      quality: 1,
      includeExtra: true,
    });
    console.log(result);
    if (result.didCancel) {
      return;
    }
    const firstAsset = result.assets?.[0];
    if (!firstAsset?.uri) {
      return;
    }

    const path =
      Platform.OS === 'ios' && firstAsset.id
        ? `ph://${firstAsset.id}`
        : firstAsset.uri;

    const loc = await getLatLong(path);
    console.log(result, loc);
    setValue({
      ...value,
      latitude: loc?.latitude ?? 0,
      longitude: loc?.longitude ?? 0,
      uri: firstAsset.uri,
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.title}>Image Picker</Text>
        <TouchableOpacity onPress={() => {}} style={styles.preview}>
          {value.uri ? (
            <Image source={{ uri: value.uri }} style={styles.image} />
          ) : (
            <View style={[styles.image, styles.imagePlaceholder]} />
          )}
        </TouchableOpacity>
        <Text style={styles.coords}>
          {value.latitude}/{value.longitude}
        </Text>
        <View style={styles.actions}>
          <Button onPress={selectFromCamera} loading={false}>
            Camera
          </Button>
          <Button onPress={selectFromLibrary} loading={false}>
            Galeria
          </Button>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f2f3f7',
    paddingHorizontal: 16,
  },
  card: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 16,
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
    elevation: 3,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#111827',
    marginBottom: 12,
  },
  preview: {
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#f8f9fc',
  },
  image: {
    width: '100%',
    height: 200,
    resizeMode: 'cover',
  },
  imagePlaceholder: {
    backgroundColor: '#e3e6ef',
  },
  coords: {
    marginTop: 10,
    fontSize: 14,
    color: '#374151',
  },
  actions: {
    marginTop: 16,
    gap: 12,
  },
});
