class ApiConstants {
  static const String baseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const String transactions = '$baseUrl/transactions';
  static const String transactionExportCsv = '$baseUrl/transactions/export/csv';
  static const String transactionExportXlsx = '$baseUrl/transactions/export/xlsx';
  static const String transactionImport = '$baseUrl/transactions/import';
  static const String dashboard = '$baseUrl/dashboard';
  static const String categories = '$baseUrl/categories';
  static const String userMe = '$baseUrl/users/me';
  static const String userFcmToken = '$baseUrl/users/fcm-token';
  static const String contas = '$baseUrl/contas';
  static const String orcamentos = '$baseUrl/orcamentos';
  static const String metas = '$baseUrl/metas';
}
