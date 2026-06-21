import { useEffect, useRef } from 'react';
import {
  drawSelection,
  EditorView,
  highlightActiveLineGutter,
  highlightSpecialChars,
  keymap,
  lineNumbers,
} from '@codemirror/view';
import { EditorState } from '@codemirror/state';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { bracketMatching, indentOnInput } from '@codemirror/language';
import { json } from '@codemirror/lang-json';

interface JsonEditorProps {
  value?: string;
  onChange?: (val: string) => void;
}

const theme = EditorView.theme(
  {
    '&': {
      height: 'auto',
      minHeight: '120px',
      border: '1px solid #424242',
      borderRadius: '6px',
      fontSize: '13px',
    },
    '&.cm-focused': {
      borderColor: '#1668dc',
      outline: 'none',
    },
    '.cm-scroller': {
      overflow: 'visible',
      maxHeight: 'none',
      fontFamily: "'Consolas', 'Courier New', monospace",
    },
    '.cm-gutters': {
      backgroundColor: '#1e1e1e',
      color: '#858585',
      border: 'none',
    },
    '.cm-activeLineGutter': {
      backgroundColor: '#2a2d2e',
    },
    '.cm-activeLine': {
      backgroundColor: '#2a2d2e33',
    },
    '.cm-content': {
      caretColor: '#d4d4d4',
    },
  },
  { dark: true },
);

function JsonEditor({ value, onChange }: JsonEditorProps): JSX.Element {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const skipNotifyRef = useRef(false);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const view = new EditorView({
      state: EditorState.create({
        doc: value || '',
        extensions: [
          lineNumbers(),
          highlightActiveLineGutter(),
          highlightSpecialChars(),
          drawSelection(),
          keymap.of(defaultKeymap),
          history(),
          keymap.of(historyKeymap),
          indentOnInput(),
          bracketMatching(),
          json(),
          theme,
          EditorView.updateListener.of((update) => {
            if (update.docChanged && !skipNotifyRef.current) {
              onChange?.(update.state.doc.toString());
            }
          }),
        ],
      }),
      parent: container,
    });

    viewRef.current = view;

    return () => {
      view.destroy();
      viewRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const view = viewRef.current;
    if (!view || value === undefined) return;
    const currentDoc = view.state.doc.toString();
    if (value !== currentDoc) {
      skipNotifyRef.current = true;
      view.dispatch({
        changes: { from: 0, to: currentDoc.length, insert: value },
      });
      skipNotifyRef.current = false;
    }
  }, [value]);

  return <div ref={containerRef} />;
}

export default JsonEditor;
