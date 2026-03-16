class ApiConstants {
  // Desenvolvimento local
  static const String baseUrl = 'http://localhost:8080';

  // Produção (Cloud Run) — substitua pela URL gerada no deploy
  // static const String baseUrl = 'https://finance-system-xxxx-uc.a.run.app';

  static const String register = '$baseUrl/auth/register';
  static const String login = '$baseUrl/auth/login';
  static const String refresh = '$baseUrl/auth/refresh';
  static const String logout = '$baseUrl/auth/logout';
  static const String transactions = '$baseUrl/transactions';
  static const String transactionExportCsv = '$baseUrl/transactions/export/csv';
  static const String dashboard = '$baseUrl/dashboard';
  static const String categories = '$baseUrl/categories';
  static const String userMe = '$baseUrl/users/me';
  static const String contas = '$baseUrl/contas';
}
