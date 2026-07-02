import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('AgentChat handleAbort (static verification)', () => {
  it('should import stopChat from session service', () => {
    const source = readFileSync(resolve(__dirname, '../AgentChat.tsx'), 'utf-8');
    expect(source).toContain("import {");
    expect(source).toContain("stopChat");
    expect(source).toContain("} from '../../services/session'");
  });

  it('handleAbort should call stopChat with sessionId', () => {
    const source = readFileSync(resolve(__dirname, '../AgentChat.tsx'), 'utf-8');
    expect(source).toContain("stopChat(sessionId)");
    expect(source).toContain(".catch(() => {})");
  });

  it('handleAbort should set toolAbortRef and call abortRef.abort after stopChat', () => {
    const source = readFileSync(resolve(__dirname, '../AgentChat.tsx'), 'utf-8');
    expect(source).toContain("toolAbortRef.current = true");
    expect(source).toContain("abortRef.current.abort()");
  });

  it('stopChat should be fire-and-forget (no await)', () => {
    const source = readFileSync(resolve(__dirname, '../AgentChat.tsx'), 'utf-8');
    const handleAbortBlock = source.match(/const handleAbort[\s\S]*?}, \[sessionId\]\);/);
    expect(handleAbortBlock).not.toBeNull();
    if (handleAbortBlock) {
      expect(handleAbortBlock[0]).toContain("stopChat(sessionId).catch(() => {})");
      expect(handleAbortBlock[0]).not.toContain("await stopChat");
    }
  });
});
