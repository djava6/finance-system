import 'package:finance_app/core/models/transaction_model.dart';
import 'package:finance_app/core/providers/auth_provider.dart';
import 'package:finance_app/core/services/api_client.dart';
import 'package:finance_app/features/accounts/account_list_screen.dart';
import 'package:finance_app/features/budgets/budget_list_screen.dart';
import 'package:finance_app/features/goals/goal_list_screen.dart';
import 'package:finance_app/features/profile/profile_screen.dart';
import 'package:finance_app/features/transactions/edit_transaction_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'helpers/mock_auth_provider.dart';
import 'helpers/mock_http.dart';

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR');
  });

  setUp(() {
    ApiClient.testClient = createMockHttpClient();
  });

  tearDown(() {
    ApiClient.testClient = null;
  });

  Widget _screen(Widget child) {
    return ChangeNotifierProvider<AuthProvider>(
      create: (_) => MockAuthProvider(),
      child: MaterialApp(
        locale: const Locale('pt', 'BR'),
        home: child,
      ),
    );
  }

  final _transacaoFixture = TransactionModel(
    id: 1,
    descricao: 'Salário',
    valor: 3000.00,
    tipo: 'RECEITA',
    data: DateTime(2026, 3, 1),
    recorrente: false,
  );

  final _transacaoRecorrenteFixture = TransactionModel(
    id: 3,
    descricao: 'Aluguel',
    valor: 1000.00,
    tipo: 'DESPESA',
    data: DateTime(2026, 3, 10),
    recorrente: true,
    frequencia: 'MENSAL',
  );

  // ── 1. EditTransactionScreen ──────────────────────────────────────────────────

  group('EditTransactionScreen', () {
    testWidgets('exibe dados pré-preenchidos da transação', (tester) async {
      await tester.pumpWidget(
          _screen(EditTransactionScreen(transaction: _transacaoFixture)));
      await tester.pumpAndSettle();

      expect(find.text('Editar transação'), findsOneWidget);
      expect(find.text('Salário'), findsOneWidget);
    });

    testWidgets('exibe toggle de recorrência ativado em transação recorrente',
        (tester) async {
      await tester.pumpWidget(_screen(
          EditTransactionScreen(transaction: _transacaoRecorrenteFixture)));
      await tester.pumpAndSettle();

      final sw = tester.widget<Switch>(find.byType(Switch));
      expect(sw.value, isTrue);
      expect(find.text('Frequência'), findsOneWidget);
    });

    testWidgets('validação impede salvar sem descrição', (tester) async {
      await tester.pumpWidget(
          _screen(EditTransactionScreen(transaction: _transacaoFixture)));
      await tester.pumpAndSettle();

      await tester.enterText(
          find.widgetWithText(TextFormField, 'Descrição'), '');
      await tester.ensureVisible(find.text('Salvar alterações'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Salvar alterações'));
      await tester.pump();
      await tester.pump();

      expect(find.textContaining('Informe a descrição'), findsOneWidget);
    });

    testWidgets('submit válido atualiza transação', (tester) async {
      await tester.pumpWidget(
        ChangeNotifierProvider<AuthProvider>(
          create: (_) => MockAuthProvider(),
          child: MaterialApp(
            home: Builder(
              builder: (ctx) => TextButton(
                onPressed: () => Navigator.push(
                  ctx,
                  MaterialPageRoute(
                    builder: (_) =>
                        EditTransactionScreen(transaction: _transacaoFixture),
                  ),
                ),
                child: const Text('Abrir'),
              ),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Abrir'));
      await tester.pumpAndSettle();

      await tester.enterText(
          find.widgetWithText(TextFormField, 'Descrição'), 'Salário Atualizado');
      await tester.ensureVisible(find.text('Salvar alterações'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Salvar alterações'));
      await tester.pumpAndSettle();

      expect(find.text('Salvar alterações'), findsNothing);
    });
  });

  // ── 2. ProfileScreen ──────────────────────────────────────────────────────────

  group('ProfileScreen', () {
    testWidgets('exibe nome e email do usuário mockado', (tester) async {
      await tester.pumpWidget(_screen(const ProfileScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('Meu Perfil'), findsOneWidget);
      expect(find.textContaining('teste@exemplo.com'), findsOneWidget);
    });

    testWidgets('validação impede salvar sem nome', (tester) async {
      await tester.pumpWidget(_screen(const ProfileScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      await tester.enterText(find.widgetWithText(TextFormField, 'Nome'), '');
      await tester.ensureVisible(find.text('Salvar alterações'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Salvar alterações'));
      await tester.pump();
      await tester.pump();

      expect(find.textContaining('Informe o nome'), findsOneWidget);
    });
  });

  // ── 3. AccountListScreen ──────────────────────────────────────────────────────

  group('AccountListScreen', () {
    testWidgets('exibe lista de contas e patrimônio total', (tester) async {
      await tester.pumpWidget(_screen(const AccountListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('Contas'), findsOneWidget);
      expect(find.text('Patrimônio total'), findsOneWidget);
      expect(find.text('Nubank'), findsOneWidget);
    });

    testWidgets('botão + abre diálogo de nova conta', (tester) async {
      await tester.pumpWidget(_screen(const AccountListScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.text('Nova conta'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Nome'), findsOneWidget);
    });

    testWidgets('ícone de editar abre diálogo com dados preenchidos',
        (tester) async {
      await tester.pumpWidget(_screen(const AccountListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.edit_outlined));
      await tester.pumpAndSettle();

      expect(find.text('Editar conta'), findsOneWidget);
      expect(find.text('Nubank'), findsOneWidget);
    });
  });

  // ── 4. BudgetListScreen ───────────────────────────────────────────────────────

  group('BudgetListScreen', () {
    testWidgets('exibe orçamentos carregados', (tester) async {
      await tester.pumpWidget(_screen(const BudgetListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('Alimentação'), findsOneWidget);
      expect(find.text('Transporte'), findsOneWidget);
    });

    testWidgets('botão + abre diálogo de novo orçamento', (tester) async {
      await tester.pumpWidget(_screen(const BudgetListScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.text('Novo orçamento'), findsOneWidget);
    });

    testWidgets('navega para mês anterior', (tester) async {
      await tester.pumpWidget(_screen(const BudgetListScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.chevron_left));
      await tester.pumpAndSettle();

      // Após navegar, o título muda — deve conter o ano
      expect(find.textContaining('2026'), findsOneWidget);
    });
  });

  // ── 5. GoalListScreen ─────────────────────────────────────────────────────────

  group('GoalListScreen', () {
    testWidgets('exibe metas carregadas', (tester) async {
      await tester.pumpWidget(_screen(const GoalListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('Metas Financeiras'), findsOneWidget);
      expect(find.text('Reserva de emergência'), findsOneWidget);
      expect(find.text('Viagem'), findsOneWidget);
    });

    testWidgets('exibe ícone de concluída na meta 100%', (tester) async {
      await tester.pumpWidget(_screen(const GoalListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.check_circle), findsOneWidget);
    });

    testWidgets('botão + abre diálogo de nova meta', (tester) async {
      await tester.pumpWidget(_screen(const GoalListScreen()));
      await tester.pumpAndSettle();

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      expect(find.text('Nova meta'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Nome'), findsOneWidget);
    });

    testWidgets('botão de depósito abre diálogo', (tester) async {
      await tester.pumpWidget(_screen(const GoalListScreen()));
      await tester.pump();
      await tester.pumpAndSettle();

      // Reserva de emergência não está concluída — tem botão de depósito
      await tester.tap(find.byIcon(Icons.add_circle_outline).first);
      await tester.pumpAndSettle();

      expect(find.textContaining('Depositar'), findsOneWidget);
    });
  });
}
