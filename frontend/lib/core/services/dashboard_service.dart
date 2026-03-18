import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/dashboard_model.dart';
import 'api_client.dart';

class DashboardService {
  final _client = ApiClient();

  Future<DashboardModel> getDashboard() async {
    final response = await _client.get(Uri.parse(ApiConstants.dashboard));
    if (response.statusCode == 200) {
      return DashboardModel.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception('Erro ao carregar dashboard (HTTP ${response.statusCode})');
  }
}
