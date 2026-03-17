import 'dart:io';
import 'dart:typed_data';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

Future<String> saveFile(List<int> bytes, String filename) async {
  final dir = await getTemporaryDirectory();
  final file = File('${dir.path}/$filename');
  await file.writeAsBytes(Uint8List.fromList(bytes));
  await Share.shareXFiles([XFile(file.path, mimeType: 'text/csv')]);
  return filename;
}
