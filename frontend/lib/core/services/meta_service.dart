import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/meta_model.dart';
import 'api_client.dart';

class MetaService {
  final _client = ApiClient();

  Future<List<MetaModel>> listar() async {
    final response = await _client.get(Uri.parse(ApiConstants.metas));
    if (response.statusCode == 200) {
      final list = jsonDecode(response.body) as List;
      return list.map((e) => MetaModel.fromJson(e as Map<String, dynamic>)).toList();
    }
    throw Exception('Erro ao carregar metas');
  }

  Future<MetaModel> criar({
    required String nome,
    required double valorAlvo,
    String? prazo,
  }) async {
    final body = <String, dynamic>{'nome': nome, 'valorAlvo': valorAlvo};
    if (prazo != null) body['prazo'] = prazo;

    final response = await _client.post(
      Uri.parse(ApiConstants.metas),
      body: jsonEncode(body),
    );
    if (response.statusCode == 201) {
      return MetaModel.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception(_extractError(response.body));
  }

  Future<MetaModel> depositar(int id, double valor) async {
    final response = await _client.patch(
      Uri.parse('${ApiConstants.metas}/$id/deposito'),
      body: jsonEncode({'valor': valor}),
    );
    if (response.statusCode == 200) {
      return MetaModel.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(int id) async {
    await _client.delete(Uri.parse('${ApiConstants.metas}/$id'));
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
