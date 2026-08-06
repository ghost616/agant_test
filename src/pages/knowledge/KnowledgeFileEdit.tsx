import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Input, message, Space } from 'antd';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { getKnowledgeFile, updateKnowledgeFile } from '../../services/knowledge';

const PANEL_PADDING = 16;

/**
 * 知识文件内容编辑页面。
 * 左右分栏布局：左侧 TextArea 编辑 Markdown，右侧 react-markdown 实时预览。
 * 底部提供保存（调用 updateKnowledgeFile）与关闭（返回文件列表）按钮。
 */
function KnowledgeFileEdit(): JSX.Element {
  const navigate = useNavigate();
  const { kbId, fileId } = useParams<{ kbId: string; fileId: string }>();
  const [fileName, setFileName] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!kbId || !fileId) return;
    setLoading(true);
    getKnowledgeFile(kbId, fileId)
      .then((file) => {
        setFileName(file.fileName);
        setContent(file.fileContent || '');
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
      await updateKnowledgeFile(kbId, fileId, { fileContent: content });
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
      <Space style={{ marginBottom: 16 }} wrap>
        <Button onClick={handleClose}>返回文件列表</Button>
        {fileName && (
          <span style={{ fontWeight: 600 }}>{fileName}</span>
        )}
      </Space>

      <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <Input.TextArea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="请输入 Markdown 内容"
            rows={24}
            style={{ fontFamily: 'monospace', fontSize: 14, lineHeight: 1.7 }}
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
            maxHeight: 640,
          }}
        >
          {content.trim() ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
          ) : (
            <span style={{ color: '#6a6a6a' }}>预览区域</span>
          )}
        </div>
      </div>

      <Space>
        <Button type="primary" loading={saving} disabled={loading} onClick={handleSave}>
          保存
        </Button>
        <Button onClick={handleClose}>关闭</Button>
      </Space>
    </div>
  );
}

export default KnowledgeFileEdit;
