import 'package:flutter/material.dart';

/// Affiche une boîte de dialogue non-annulable pendant la génération, dont le
/// texte suit la callback `onProgress` du générateur.
class GenerationProgressController {
  final ValueNotifier<String> stepNotifier = ValueNotifier('Démarrage…');

  void update(String step) => stepNotifier.value = step;

  void dispose() => stepNotifier.dispose();
}

Future<void> showGenerationProgressDialog(
  BuildContext context,
  GenerationProgressController controller,
) {
  return showDialog(
    context: context,
    barrierDismissible: false,
    builder: (context) => AlertDialog(
      content: Row(
        children: [
          const CircularProgressIndicator(),
          const SizedBox(width: 24),
          Expanded(
            child: ValueListenableBuilder<String>(
              valueListenable: controller.stepNotifier,
              builder: (context, value, _) => Text(value),
            ),
          ),
        ],
      ),
    ),
  );
}
