import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/transaction_model.dart';
import 'api_client.dart';

class TransactionPage {
  final List<TransactionModel> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  TransactionPage({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  factory TransactionPage.fromJson(Map<String, dynamic> json) => TransactionPage(
        content: (json['content'] as List)
            .map((e) => TransactionModel.fromJson(e as Map<String, dynamic>))
            .toList(),
        page: json['page'] as int,
        size: json['size'] as int,
        totalElements: json['totalElements'] as int,
        totalPages: json['totalPages'] as int,
      );
}

class TransactionService {
  final _client = ApiClient();

  Future<TransactionPage> listar(
      {DateTime? inicio, DateTime? fim, int page = 0, int size = 20}) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (inicio != null && fim != null) {
      params['inicio'] = '${inicio.year.toString().padLeft(4, '0')}-'
          '${inicio.month.toString().padLeft(2, '0')}-'
          '${inicio.day.toString().padLeft(2, '0')}';
      params['fim'] = '${fim.year.toString().padLeft(4, '0')}-'
          '${fim.month.toString().padLeft(2, '0')}-'
          '${fim.day.toString().padLeft(2, '0')}';
    }
    final uri = Uri.parse(ApiConstants.transactions).replace(queryParameters: params);
    final response = await _client.get(uri);
    if (response.statusCode == 200) {
      return TransactionPage.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception('Erro ao carregar transações');
  }

  Future<TransactionModel> criar({
    required String descricao,
    required double valor,
    required String tipo,
    int? categoriaId,
    int? contaId,
  }) async {
    final response = await _client.post(
      Uri.parse(ApiConstants.transactions),
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
    required int id,
    required String descricao,
    required double valor,
    required String tipo,
    int? categoriaId,
    int? contaId,
  }) async {
    final response = await _client.put(
      Uri.parse('${ApiConstants.transactions}/$id'),
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

  Future<void> deletar(int id) async {
    final response = await _client.delete(
      Uri.parse('${ApiConstants.transactions}/$id'),
    );
    if (response.statusCode != 204) {
      throw Exception('Erro ao excluir transação');
    }
  }

  Future<List<int>> exportarCsv() async {
    final response = await _client.get(
      Uri.parse(ApiConstants.transactionExportCsv),
    );
    if (response.statusCode == 200) {
      return response.bodyBytes.toList();
    }
    throw Exception('Erro ao exportar CSV');
  }

  Future<Map<String, dynamic>> importarCsv(List<int> fileBytes, String fileName) async {
    final response = await _client.postMultipart(
      Uri.parse(ApiConstants.transactionImport),
      fileBytes: fileBytes,
      fileName: fileName,
      fieldName: 'file',
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body) as Map<String, dynamic>;
    }
    throw Exception('Erro ao importar CSV');
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
