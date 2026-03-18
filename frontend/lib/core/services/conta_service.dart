import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/conta_model.dart';
import 'api_client.dart';

class ContaService {
  Future<List<ContaModel>> listar(String token) async {
    final client = ApiClient(token);
    final response = await client.get(Uri.parse(ApiConstants.contas));
    if (response.statusCode == 200) {
      final list = jsonDecode(response.body) as List;
      return list.map((e) => ContaModel.fromJson(e)).toList();
    }
    throw Exception('Erro ao carregar contas');
  }

  Future<ContaModel> criar(String token, String nome, double saldo) async {
    final client = ApiClient(token);
    final response = await client.post(
      Uri.parse(ApiConstants.contas),
      body: jsonEncode({'nome': nome, 'saldo': saldo}),
    );
    if (response.statusCode == 201) {
      return ContaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<ContaModel> atualizar(
      String token, int id, String nome, double saldo) async {
    final client = ApiClient(token);
    final response = await client.put(
      Uri.parse('${ApiConstants.contas}/$id'),
      body: jsonEncode({'nome': nome, 'saldo': saldo}),
    );
    if (response.statusCode == 200) {
      return ContaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(String token, int id) async {
    final client = ApiClient(token);
    final response = await client.delete(Uri.parse('${ApiConstants.contas}/$id'));
    if (response.statusCode != 204) {
      throw Exception(_extractError(response.body));
    }
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
