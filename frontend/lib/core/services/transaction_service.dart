import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/transaction_model.dart';

class TransactionService {
  Map<String, String> _headers(String token) => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      };

  Future<List<TransactionModel>> listar(String token,
      {DateTime? inicio, DateTime? fim}) async {
    String url = ApiConstants.transactions;
    if (inicio != null && fim != null) {
      final i = '${inicio.year.toString().padLeft(4, '0')}-'
          '${inicio.month.toString().padLeft(2, '0')}-'
          '${inicio.day.toString().padLeft(2, '0')}';
      final f = '${fim.year.toString().padLeft(4, '0')}-'
          '${fim.month.toString().padLeft(2, '0')}-'
          '${fim.day.toString().padLeft(2, '0')}';
      url = '$url?inicio=$i&fim=$f';
    }
    final response = await http.get(Uri.parse(url), headers: _headers(token));
    if (response.statusCode == 200) {
      final List list = jsonDecode(response.body) as List;
      return list
          .map((e) => TransactionModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    throw Exception('Erro ao carregar transações');
  }

  Future<TransactionModel> criar({
    required String token,
    required String descricao,
    required double valor,
    required String tipo,
    int? categoriaId,
    int? contaId,
  }) async {
    final response = await http.post(
      Uri.parse(ApiConstants.transactions),
      headers: _headers(token),
      body: jsonEncode({
        'descricao': descricao,
        'valor': valor,
        'tipo': tipo,
        if (categoriaId != null) 'categoriaId': categoriaId,
        if (contaId != null) 'contaId': contaId,
      }),
    );
    if (response.statusCode == 201) {
      return TransactionModel.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception('Erro ao criar transação');
  }

  Future<TransactionModel> atualizar({
    required String token,
    required int id,
    required String descricao,
    required double valor,
    required String tipo,
    int? categoriaId,
    int? contaId,
  }) async {
    final response = await http.put(
      Uri.parse('${ApiConstants.transactions}/$id'),
      headers: _headers(token),
      body: jsonEncode({
        'descricao': descricao,
        'valor': valor,
        'tipo': tipo,
        if (categoriaId != null) 'categoriaId': categoriaId,
        if (contaId != null) 'contaId': contaId,
      }),
    );
    if (response.statusCode == 200) {
      return TransactionModel.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(String token, int id) async {
    final response = await http.delete(
      Uri.parse('${ApiConstants.transactions}/$id'),
      headers: _headers(token),
    );
    if (response.statusCode != 204) {
      throw Exception('Erro ao excluir transação');
    }
  }

  Future<List<int>> exportarCsv(String token) async {
    final response = await http.get(
      Uri.parse(ApiConstants.transactionExportCsv),
      headers: _headers(token),
    );
    if (response.statusCode == 200) {
      return response.bodyBytes.toList();
    }
    throw Exception('Erro ao exportar CSV');
  }

  String _extractError(String body) {
    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      return json['erro'] as String? ?? 'Erro desconhecido';
    } catch (_) {
      return 'Erro ao conectar com o servidor';
    }
  }
}
