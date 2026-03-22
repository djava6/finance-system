import 'package:finance_app/core/providers/auth_provider.dart';
import 'package:finance_app/features/auth/login_screen.dart';
import 'package:finance_app/features/categories/category_list_screen.dart';
import 'package:finance_app/features/dashboard/dashboard_screen.dart';
import 'package:finance_app/features/transactions/add_transaction_screen.dart';
import 'package:finance_app/features/transactions/transaction_list_screen.dart';
import 'package:finance_app/core/services/api_client.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'helpers/mock_auth_provider.dart';
import 'helpers/mock_http.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(() async {
    await initializeDateFormatting('pt_BR');
  });

  setUp(() {
    ApiClient.testClient = createMockHttpClient();
  });

  tearDown(() {
    ApiClient.testClient = null;
  });

  // ── Helpers ──────────────────────────────────────────────────────────────────

  Widget _screen(Widget child, {bool authenticated = true}) {
    return ChangeNotifierProvider<AuthProvider>(
      create: (_) => MockAuthProvider(authenticated: authenticated),
      child: MaterialApp(
        locale: const Locale('pt', 'BR'),
        home: child,
      ),
    );
  }

  // ── 1. Login ─────────────────────────────────────────────────────────────────

  group('LoginScreen', () {
    testWidgets('exibe botões de login quando não autenticado', (tester) async {
      await tester.pumpWidget(_screen(const LoginScreen(), authenticated: false));
      await tester.pumpAndSettle();

      expect(find.textContaining('Google'), findsOneWidget);
      expect(find.byIcon(Icons.lock_outline), findsNothing);
    });
  });

  // ── 2. Dashboard ─────────────────────────────────────────────────────────────

  group('DashboardScreen', () {
    testWidgets('carrega e exibe saldo com dados mockados', (tester) async {
      await tester.pumpWidget(_screen(const DashboardScreen()));
      await tester.pump();           // inicia carregamento
      await tester.pumpAndSettle();  // aguarda Future

      expect(find.text('Saldo atual'), findsOneWidget);
      expect(find.textContaining('R\$'), findsWidgets);
    });

    testWidgets('navega para Transações pela bottom bar', (tester) async {
      await tester.pumpWidget(_screen(const DashboardScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.list_alt_outlined));
      await tester.pumpAndSettle();

      expect(find.text('Transações'), findsWidgets);
    });

    testWidgets('navega para Categorias pela bottom bar', (tester) async {
      await tester.pumpWidget(_screen(const DashboardScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.label_outlined));
      await tester.pumpAndSettle();

      expect(find.text('Categorias'), findsOneWidget);
    });
  });

  // ── 3. Transações ─────────────────────────────────────────────────────────────

  group('TransactionListScreen', () {
    testWidgets('exibe lista de transações', (tester) async {
      await tester.pumpWidget(_screen(const TransactionListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('Salário'), findsOneWidget);
      expect(find.text('Supermercado'), findsOneWidget);
    });

    testWidgets('exibe ícone de recorrente na transação recorrente', (tester) async {
      await tester.pumpWidget(_screen(const TransactionListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      // "Aluguel" é recorrente — deve ter ícone repeat
      expect(find.text('Aluguel'), findsOneWidget);
      expect(find.byIcon(Icons.repeat), findsOneWidget);
    });

    testWidgets('botão de filtro abre date range picker', (tester) async {
      await tester.pumpWidget(_screen(const TransactionListScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.date_range));
      await tester.pumpAndSettle();

      // DateRangePicker exibe navegação de meses
      expect(find.byType(DateRangePickerDialog), findsOneWidget);
    });
  });

  // ── 4. Nova transação ─────────────────────────────────────────────────────────

  group('AddTransactionScreen', () {
    testWidgets('validação impede salvar sem descrição', (tester) async {
      await tester.pumpWidget(_screen(const AddTransactionScreen()));
      await tester.pumpAndSettle();

      // Toca Salvar sem preencher
      await tester.tap(find.text('Salvar'));
      await tester.pumpAndSettle();

      expect(find.text('Informe a descrição'), findsOneWidget);
    });

    testWidgets('validação impede salvar sem valor', (tester) async {
      await tester.pumpWidget(_screen(const AddTransactionScreen()));
      await tester.pumpAndSettle();

      await tester.enterText(
          find.widgetWithText(TextFormField, 'Descrição'), 'Teste');
      await tester.tap(find.text('Salvar'));
      await tester.pumpAndSettle();

      expect(find.text('Informe o valor'), findsOneWidget);
    });

    testWidgets('toggle recorrente exibe campo de frequência', (tester) async {
      await tester.pumpWidget(_screen(const AddTransactionScreen()));
      await tester.pumpAndSettle();

      // Frequência não visível ainda
      expect(find.text('Frequência'), findsNothing);

      await tester.tap(find.byType(Switch));
      await tester.pumpAndSettle();

      expect(find.text('Frequência'), findsOneWidget);
    });

    testWidgets('submit válido cria transação', (tester) async {
      await tester.pumpWidget(_screen(const AddTransactionScreen()));
      await tester.pumpAndSettle();

      await tester.enterText(
          find.widgetWithText(TextFormField, 'Descrição'), 'Teste E2E');
      await tester.enterText(
          find.widgetWithText(TextFormField, 'Valor (R\$)'), '100.00');
      await tester.tap(find.text('Salvar'));
      await tester.pumpAndSettle();

      // Após salvar, tela fecha (pop) — não encontra mais o botão
      expect(find.text('Salvar'), findsNothing);
    });
  });

  // ── 5. Categorias ─────────────────────────────────────────────────────────────

  group('CategoryListScreen', () {
    testWidgets('exibe lista de categorias mockadas', (tester) async {
      await tester.pumpWidget(_screen(const CategoryListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('Alimentação'), findsOneWidget);
      expect(find.text('Moradia'), findsOneWidget);
      expect(find.text('Transporte'), findsOneWidget);
    });

    testWidgets('campo de nova categoria aceita texto', (tester) async {
      await tester.pumpWidget(_screen(const CategoryListScreen()));
      await tester.pumpAndSettle();

      // Abre o diálogo de nova categoria
      await tester.tap(find.byIcon(Icons.add));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField), 'Saúde');
      expect(find.text('Saúde'), findsOneWidget);
    });
  });
}
