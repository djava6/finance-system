import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../core/models/categoria_model.dart';
import '../../core/models/conta_model.dart';
import '../../core/models/transaction_model.dart';
import '../../core/services/categoria_service.dart';
import '../../core/services/conta_service.dart';
import '../../core/services/recibo_service.dart';
import '../../core/services/transaction_service.dart';

class EditTransactionScreen extends StatefulWidget {
  final TransactionModel transaction;

  const EditTransactionScreen({super.key, required this.transaction});

  @override
  State<EditTransactionScreen> createState() => _EditTransactionScreenState();
}

class _EditTransactionScreenState extends State<EditTransactionScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _descricaoController;
  late final TextEditingController _valorController;
  late String _tipo;
  int? _categoriaId;
  int? _contaId;
  List<CategoriaModel> _categorias = [];
  List<ContaModel> _contas = [];
  bool _loading = false;
  late bool _recorrente;
  String? _frequencia;

  final _service = TransactionService();
  final _categoriaService = CategoriaService();
  final _contaService = ContaService();
  final _reciboService = ReciboService();
  bool _uploadingRecibo = false;
  String? _reciboUrl;

  @override
  void initState() {
    super.initState();
    _descricaoController =
        TextEditingController(text: widget.transaction.descricao);
    _valorController =
        TextEditingController(text: widget.transaction.valor.toStringAsFixed(2));
    _tipo = widget.transaction.tipo;
    _reciboUrl = widget.transaction.reciboUrl;
    _recorrente = widget.transaction.recorrente;
    _frequencia = widget.transaction.frequencia;
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final results = await Future.wait([
        _categoriaService.listar(),
        _contaService.listar(),
      ]);
      if (mounted) {
        setState(() {
          _categorias = results[0] as List<CategoriaModel>;
          _contas = results[1] as List<ContaModel>;
          _categoriaId = widget.transaction.categoriaId;
          _contaId = widget.transaction.contaId;
        });
      }
    } catch (_) {}
  }

  @override
  void dispose() {
    _descricaoController.dispose();
    _valorController.dispose();
    super.dispose();
  }

  Future<void> _uploadRecibo() async {
    setState(() => _uploadingRecibo = true);
    try {
      final updated = await _reciboService.uploadAndSave(widget.transaction.id);
      if (updated != null && mounted) {
        setState(() => _reciboUrl = updated.reciboUrl);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Recibo salvo com sucesso')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _uploadingRecibo = false);
    }
  }

  Future<void> _salvar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);

    try {
      await _service.atualizar(
        id: widget.transaction.id,
        descricao: _descricaoController.text.trim(),
        valor: double.parse(_valorController.text.replaceAll(',', '.')),
        tipo: _tipo,
        categoriaId: _categoriaId,
        contaId: _contaId,
        recorrente: _recorrente,
        frequencia: _recorrente ? _frequencia : null,
      );
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Editar transação')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(
                      value: 'RECEITA',
                      label: Text('Receita'),
                      icon: Icon(Icons.arrow_upward)),
                  ButtonSegment(
                      value: 'DESPESA',
                      label: Text('Despesa'),
                      icon: Icon(Icons.arrow_downward)),
                ],
                selected: {_tipo},
                onSelectionChanged: (s) => setState(() => _tipo = s.first),
              ),
              const SizedBox(height: 20),
              TextFormField(
                controller: _descricaoController,
                decoration: const InputDecoration(
                  labelText: 'Descrição',
                  prefixIcon: Icon(Icons.description_outlined),
                  border: OutlineInputBorder(),
                ),
                validator: (v) =>
                    v == null || v.isEmpty ? 'Informe a descrição' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _valorController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Valor (R\$)',
                  prefixIcon: Icon(Icons.attach_money),
                  border: OutlineInputBorder(),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Informe o valor';
                  final parsed = double.tryParse(v.replaceAll(',', '.'));
                  if (parsed == null || parsed <= 0) return 'Valor inválido';
                  return null;
                },
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                value: _categoriaId,
                decoration: const InputDecoration(
                  labelText: 'Categoria (opcional)',
                  prefixIcon: Icon(Icons.label_outline),
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Sem categoria')),
                  ..._categorias.map((c) => DropdownMenuItem(
                        value: c.id,
                        child: Text(c.nome),
                      )),
                ],
                onChanged: (v) => setState(() => _categoriaId = v),
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                value: _contaId,
                decoration: const InputDecoration(
                  labelText: 'Conta (opcional)',
                  prefixIcon: Icon(Icons.account_balance_outlined),
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Sem conta')),
                  ..._contas.map((c) => DropdownMenuItem(
                        value: c.id,
                        child: Text(c.nome),
                      )),
                ],
                onChanged: (v) => setState(() => _contaId = v),
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Transação recorrente'),
                secondary: const Icon(Icons.repeat),
                value: _recorrente,
                onChanged: (v) => setState(() {
                  _recorrente = v;
                  if (!v) _frequencia = null;
                }),
              ),
              if (_recorrente) ...[
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  value: _frequencia,
                  decoration: const InputDecoration(
                    labelText: 'Frequência',
                    prefixIcon: Icon(Icons.calendar_today_outlined),
                    border: OutlineInputBorder(),
                  ),
                  items: const [
                    DropdownMenuItem(value: 'SEMANAL', child: Text('Semanal')),
                    DropdownMenuItem(value: 'MENSAL', child: Text('Mensal')),
                    DropdownMenuItem(value: 'ANUAL', child: Text('Anual')),
                  ],
                  onChanged: (v) => setState(() => _frequencia = v),
                  validator: (v) =>
                      _recorrente && (v == null || v.isEmpty) ? 'Selecione a frequência' : null,
                ),
              ],
              const SizedBox(height: 24),
              _ReciboWidget(
                url: _reciboUrl,
                uploading: _uploadingRecibo,
                onUpload: _uploadRecibo,
              ),
              const SizedBox(height: 32),
              FilledButton.icon(
                onPressed: _loading ? null : _salvar,
                icon: _loading
                    ? const SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.check),
                label: const Text('Salvar alterações'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ReciboWidget extends StatelessWidget {
  final String? url;
  final bool uploading;
  final VoidCallback onUpload;

  const _ReciboWidget({
    required this.url,
    required this.uploading,
    required this.onUpload,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            const Icon(Icons.receipt_long_outlined, size: 20),
            const SizedBox(width: 8),
            const Text('Recibo', style: TextStyle(fontWeight: FontWeight.w600)),
            const Spacer(),
            if (uploading)
              const SizedBox(
                  width: 20, height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2))
            else
              TextButton.icon(
                onPressed: onUpload,
                icon: const Icon(Icons.upload_outlined, size: 18),
                label: Text(url == null ? 'Anexar' : 'Substituir'),
              ),
          ],
        ),
        if (url != null)
          InkWell(
            onTap: () => launchUrl(Uri.parse(url!),
                mode: LaunchMode.externalApplication),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: BoxDecoration(
                border: Border.all(color: Colors.green.shade300),
                borderRadius: BorderRadius.circular(8),
                color: Colors.green.shade50,
              ),
              child: Row(
                children: [
                  Icon(Icons.check_circle_outline,
                      color: Colors.green.shade700, size: 18),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text('Ver recibo anexado',
                        style: TextStyle(decoration: TextDecoration.underline)),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
