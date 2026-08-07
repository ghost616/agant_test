import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Input, message, Space, Tag } from 'antd';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {
  getKnowledgeFile,
  getKnowledgeFileContent,
  updateKnowledgeFileContent,
} from '../../services/knowledge';

const PANEL_PADDING = 16;
const EDITOR_HEIGHT = 640;

/**
 * 知识文件内容编辑页面。
 * 左右分栏布局：左侧 TextArea 编辑 Markdown，右侧 react-markdown 实时预览。
 * 内容通过 getKnowledgeFileContent 加载、updateKnowledgeFileContent 保存。
 * 底部右下角提供保存与关闭按钮。
 */
function KnowledgeFileEdit(): JSX.Element {
  const navigate = useNavigate();
  const { kbId, fileId } = useParams<{ kbId: string; fileId: string }>();
  const [fileName, setFileName] = useState('');
  const [content, setContent] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!kbId || !fileId) return;
    setLoading(true);
    Promise.all([getKnowledgeFile(kbId, fileId), getKnowledgeFileContent(kbId, fileId)])
      .then(([file, fileContent]) => {
        setFileName(file.fileName);
        setContent(fileContent);
        setPublishing(file.publishStatus === 'PUBLISHING');
      })
      .catch(() => {
        message.error('获取文件详情失败');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [kbId, fileId]);

  const handleSave = async (): Promise<void> => {
    if (!kbId || !fileId) return;
    setSaving(true);
    try {
      await updateKnowledgeFileContent(kbId, fileId, content);
      message.success('保存成功');
    } catch {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleClose = (): void => {
    navigate(`/knowledge/${kbId}/files`);
  };

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        {fileName && (
          <Space>
            <span style={{ fontWeight: 600 }}>{fileName}</span>
            {publishing && <Tag color="processing">发布中，暂不可编辑</Tag>}
          </Space>
        )}
      </div>

      <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <Input.TextArea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="请输入 Markdown 内容"
            rows={24}
            disabled={publishing}
            style={{
              fontFamily: 'monospace',
              fontSize: 14,
              lineHeight: 1.7,
              height: EDITOR_HEIGHT,
            }}
            showCount
          />
        </div>
        <div
          style={{
            flex: 1,
            minWidth: 0,
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            padding: PANEL_PADDING,
            overflow: 'auto',
            height: EDITOR_HEIGHT,
          }}
        >
          {content.trim() ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
          ) : (
            <span style={{ color: '#6a6a6a' }}>预览区域</span>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Space>
          <Button
            type="primary"
            loading={saving}
            disabled={loading || publishing}
            onClick={handleSave}
          >
            保存
          </Button>
          <Button onClick={handleClose}>关闭</Button>
        </Space>
      </div>
    </div>
  );
}

export default KnowledgeFileEdit;
