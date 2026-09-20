import 'dart:async';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

import '../generators/quote_generator.dart';
import '../services/media_picker_service.dart';
import '../widgets/generation_progress_dialog.dart';
import '../widgets/media_picker_tile.dart';
import 'preview_screen.dart';

class QuoteScreen extends StatefulWidget {
  const QuoteScreen({super.key});

  @override
  State<QuoteScreen> createState() => _QuoteScreenState();
}

class _QuoteScreenState extends State<QuoteScreen> {
  final _textController = TextEditingController();
  final _authorController = TextEditingController();
  final _picker = const MediaPickerService();

  double _duration = 30;
  String? _backgroundImage;
  String? _backgroundVideo;
  String? _music;
  bool _generateDefaultMusic = true;
  bool _busy = false;

  @override
  void dispose() {
    _textController.dispose();
    _authorController.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    if (_textController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Écris le texte de la citation.')),
      );
      return;
    }

    setState(() => _busy = true);
    final progress = GenerationProgressController();
    unawaited(showGenerationProgressDialog(context, progress));

    try {
      final outputDir = await getApplicationDocumentsDirectory();
      final outputPath = '${outputDir.path}/tikapub_quote_${const Uuid().v4()}.mp4';

      final result = await QuoteGenerator().generate(
        QuoteGenerationRequest(
          text: _textController.text.trim(),
          author: _authorController.text.trim().isEmpty ? null : _authorController.text.trim(),
          duration: _duration,
          backgroundImagePath: _backgroundImage,
          backgroundVideoPath: _backgroundVideo,
          musicPath: _music,
          generateDefaultMusic: _generateDefaultMusic,
        ),
        outputPath,
        onProgress: progress.update,
      );

      if (!mounted) return;
      Navigator.of(context).pop(); // ferme le dialogue de progression
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => PreviewScreen(videoPath: result)),
      );
    } catch (e) {
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      progress.dispose();
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Citation')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _textController,
            maxLines: 4,
            decoration: const InputDecoration(
              labelText: 'Texte de la citation',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _authorController,
            decoration: const InputDecoration(
              labelText: 'Auteur (optionnel)',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 20),
          Text('Durée : ${_duration.round()} s'),
          Slider(
            value: _duration,
            min: 15,
            max: 90,
            divisions: 75,
            label: '${_duration.round()} s',
            onChanged: (v) => setState(() => _duration = v),
          ),
          const Divider(height: 32),
          MediaPickerTile(
            label: 'Fond image',
            selectedPath: _backgroundImage,
            icon: Icons.image,
            onPick: () async {
              final path = await _picker.pickImage();
              if (path != null) setState(() { _backgroundImage = path; _backgroundVideo = null; });
            },
            onClear: () => setState(() => _backgroundImage = null),
          ),
          MediaPickerTile(
            label: 'Fond vidéo',
            selectedPath: _backgroundVideo,
            icon: Icons.movie,
            onPick: () async {
              final path = await _picker.pickVideo();
              if (path != null) setState(() { _backgroundVideo = path; _backgroundImage = null; });
            },
            onClear: () => setState(() => _backgroundVideo = null),
          ),
          if (_backgroundImage == null && _backgroundVideo == null)
            const Padding(
              padding: EdgeInsets.only(left: 16),
              child: Text(
                'Sans fond choisi : un dégradé animé est généré automatiquement.',
                style: TextStyle(fontStyle: FontStyle.italic, fontSize: 12),
              ),
            ),
          const Divider(height: 32),
          MediaPickerTile(
            label: 'Musique',
            selectedPath: _music,
            icon: Icons.music_note,
            onPick: () async {
              final path = await _picker.pickAudio();
              if (path != null) setState(() => _music = path);
            },
            onClear: () => setState(() => _music = null),
          ),
          if (_music == null)
            SwitchListTile(
              title: const Text('Générer une musique d\'ambiance automatiquement'),
              value: _generateDefaultMusic,
              onChanged: (v) => setState(() => _generateDefaultMusic = v),
            ),
          const SizedBox(height: 24),
          FilledButton.icon(
            icon: const Icon(Icons.movie_creation),
            label: const Text('Générer la vidéo'),
            onPressed: _busy ? null : _generate,
          ),
        ],
      ),
    );
  }
}
