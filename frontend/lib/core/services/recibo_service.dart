import 'dart:io';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'transaction_service.dart';
import '../models/transaction_model.dart';

class ReciboService {
  final _txService = TransactionService();

  static const _maxBytes = 10 * 1024 * 1024; // 10 MB
  static const _allowedExtensions = ['jpg', 'jpeg', 'png', 'pdf'];

  /// Abre o seletor de arquivo, faz upload para Firebase Storage
  /// e salva a URL no backend. Retorna a transação atualizada.
  Future<TransactionModel?> uploadAndSave(int transacaoId) async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: _allowedExtensions,
      withData: kIsWeb,
    );
    if (result == null || result.files.isEmpty) return null;

    final picked = result.files.first;
    final uid = FirebaseAuth.instance.currentUser!.uid;
    final ext = picked.extension ?? 'jpg';
    final ref = FirebaseStorage.instance
        .ref('recibos/$uid/$transacaoId/recibo.$ext');

    if (kIsWeb) {
      final bytes = picked.bytes;
      if (bytes == null) throw Exception('Não foi possível ler o arquivo.');
      if (bytes.length > _maxBytes) throw Exception('Arquivo muito grande (máx 10 MB).');
      await ref.putData(bytes);
    } else {
      final file = File(picked.path!);
      final size = await file.length();
      if (size > _maxBytes) throw Exception('Arquivo muito grande (máx 10 MB).');
      await ref.putFile(file);
    }

    final url = await ref.getDownloadURL();
    return await _txService.salvarReciboUrl(transacaoId, url);
  }

  Future<void> delete(int transacaoId) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) return;
    try {
      for (final ext in _allowedExtensions) {
        try {
          await FirebaseStorage.instance
              .ref('recibos/$uid/$transacaoId/recibo.$ext')
              .delete();
          break;
        } catch (_) {}
      }
    } catch (_) {}
  }
}
