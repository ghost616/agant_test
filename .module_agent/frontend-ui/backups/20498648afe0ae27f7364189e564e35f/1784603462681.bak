import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Button, Input, message, Spin, Switch, Tag, Typography } from 'antd';
import type { ChatRequest } from '../../types/model';
import { chatStream, getModel } from '../../services/model';
import type { ModelConfig } from '../../types/model';

function ModelTest(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const [model, setModel] = useState<ModelConfig | null>(null);
  const [inputValue, setInputValue] = useState('');
  const [responseText, setResponseText] = useState('');
  const [reasoningText, setReasoningText] = useState('');
  const [loading, setLoading] = useState(false);
  const [thinking, setThinking] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);
  const responseRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);
  const calledRef = useRef(false);
  const hasResponseRef = useRef(false);
  const hasReasoningRef = useRef(false);

  useEffect(() => {
    if (!id) return;
    if (calledRef.current) return;
    calledRef.current = true;
    getModel(id)
      .then((data) => setModel(data))
      .catch(() => message.error('获取模型信息失败'))
      .finally(() => setPageLoading(false));
  }, [id]);

  useEffect(() => {
    if (responseRef.current) {
      responseRef.current.scrollTop = responseRef.current.scrollHeight;
    }
  }, [responseText]);

  const handleAbort = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
  }, []);

  useEffect(() => {
    return () => handleAbort();
  }, [handleAbort]);

  const handleSend = useCallback(() => {
    if (!id || !inputValue.trim() || !model) return;

    setLoading(true);
    setResponseText('');
    setReasoningText('');
    hasResponseRef.current = false;
    hasReasoningRef.current = false;

    const request: ChatRequest = {
      messages: [{ role: 'user', content: inputValue }],
      temperature: model.temperature,
      maxTokens: model.maxTokens,
      model: model.modelName,
      thinking,
    };

    abortRef.current = chatStream(id, request, {
      onChunk: (text: string) => {
        hasResponseRef.current = true;
        setResponseText((prev) => prev + text);
      },
      onReasoning: (text: string) => {
        hasReasoningRef.current = true;
        hasResponseRef.current = true;
        setReasoningText((prev) => prev + text);
      },
      onDone: () => {
        if (!hasResponseRef.current) {
          message.warning('模型未返回任何内容，请检查 API Key / Base URL / 模型名称');
        }
        setLoading(false);
        abortRef.current = null;
      },
      onError: (err: Error) => {
        message.error(err.message || '对话请求失败');
        setLoading(false);
        abortRef.current = null;
      },
    });

    setInputValue('');
  }, [id, inputValue, model, thinking]);

  const handleClear = useCallback(() => {
    setResponseText('');
    setReasoningText('');
  }, []);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        if (!loading) handleSend();
      }
    },
    [handleSend, loading],
  );

  if (pageLoading) {
    return (
      <div style={{ textAlign: 'center', paddingTop: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!model) {
    return (
      <div style={{ textAlign: 'center', paddingTop: 100 }}>
        <Typography.Text type="secondary">模型不存在</Typography.Text>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 180px)' }}>
      <style>{`
        .model-test-markdown pre {
          background: #2d2d2d;
          border-radius: 6px;
          padding: 12px 16px;
          overflow-x: auto;
        }
        .model-test-markdown code {
          font-family: 'Consolas', 'Courier New', monospace;
          font-size: 13px;
        }
        .model-test-markdown :not(pre) > code {
          background: #2d2d2d;
          padding: 2px 6px;
          border-radius: 4px;
        }
        .model-test-markdown table {
          border-collapse: collapse;
          width: 100%;
          margin: 12px 0;
        }
        .model-test-markdown th,
        .model-test-markdown td {
          border: 1px solid #444;
          padding: 8px 12px;
          text-align: left;
        }
        .model-test-markdown th {
          background: #2d2d2d;
          font-weight: 600;
        }
        .model-test-markdown blockquote {
          border-left: 3px solid #555;
          padding-left: 12px;
          margin: 12px 0;
          color: #aaa;
        }
        .model-test-markdown a {
          color: #569cd6;
        }
        .model-test-markdown ul,
        .model-test-markdown ol {
          padding-left: 24px;
        }
        .model-test-markdown h1,
        .model-test-markdown h2,
        .model-test-markdown h3,
        .model-test-markdown h4,
        .model-test-markdown h5,
        .model-test-markdown h6 {
          color: #e0e0e0;
          margin: 16px 0 8px;
        }
        .model-test-markdown hr {
          border: none;
          border-top: 1px solid #444;
          margin: 16px 0;
        }
        .model-test-markdown p {
          margin: 8px 0;
        }
      `}</style>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {model.name}
        </Typography.Title>
        <Tag color="blue">{model.platformType}</Tag>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
          <Typography.Text type="secondary">思考模式</Typography.Text>
          <Switch
            checked={thinking}
            onChange={setThinking}
            size="small"
          />
        </div>
      </div>

      <div
        ref={responseRef}
        style={{
          flex: 1,
          background: '#1e1e1e',
          borderRadius: 8,
          padding: 16,
          overflowY: 'auto',
          marginBottom: 16,
          minHeight: 200,
        }}
      >
        {reasoningText && (
          <div
            style={{
              background: '#252525',
              borderLeft: '3px solid #ffd700',
              borderRadius: 4,
              padding: '8px 12px',
              marginBottom: 12,
            }}
            className="model-test-markdown"
          >
            <Typography.Text
              style={{ color: '#ffd700', fontSize: 12, marginBottom: 4, display: 'block' }}
            >
              思考过程
            </Typography.Text>
            <div style={{ color: '#aaa', fontSize: 13, lineHeight: 1.7 }}>
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {reasoningText}
              </ReactMarkdown>
            </div>
          </div>
        )}
        {responseText ? (
          <div
            style={{
              color: '#d4d4d4',
              fontSize: 14,
              lineHeight: 1.8,
            }}
            className="model-test-markdown"
          >
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {responseText}
            </ReactMarkdown>
          </div>
        ) : (
          !reasoningText && (
            <Typography.Text
              style={{ color: '#6a6a6a', fontSize: 14 }}
            >
              发送消息开始测试
            </Typography.Text>
          )
        )}
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <Input.TextArea
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          rows={3}
          autoSize={{ minRows: 2, maxRows: 6 }}
        />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, minWidth: 80 }}>
          <Button
            type="primary"
            onClick={handleSend}
            disabled={loading || !inputValue.trim()}
            loading={loading}
          >
            发送
          </Button>
          <Button onClick={handleClear} disabled={loading}>
            清空
          </Button>
          {loading && (
            <Button onClick={handleAbort} danger>
              停止
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

export default ModelTest;
