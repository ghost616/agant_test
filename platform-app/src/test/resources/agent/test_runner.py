import sys
import os
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'main', 'resources', 'agent'))

from _runner import AgentExecutionContext


class TestAgentExecutionContextStopped(unittest.TestCase):
    def test_stopped_default_false(self):
        ctx = AgentExecutionContext({})
        self.assertFalse(ctx.stopped)

    def test_stopped_true_when_passed(self):
        ctx = AgentExecutionContext({"stopped": True})
        self.assertTrue(ctx.stopped)

    def test_stopped_false_when_passed_explicitly(self):
        ctx = AgentExecutionContext({"stopped": False})
        self.assertFalse(ctx.stopped)

    def test_stopped_with_full_context(self):
        data = {
            "sessionId": "s1",
            "agentId": "a1",
            "systemPrompt": "hello",
            "modelId": "m1",
            "recentMessageCount": 5,
            "history": [],
            "tools": [],
            "skills": [],
            "sessionVariables": {},
            "conversationVariables": {},
            "stopped": True
        }
        ctx = AgentExecutionContext(data)
        self.assertTrue(ctx.stopped)
        self.assertEqual(ctx.session_id, "s1")


if __name__ == "__main__":
    unittest.main()
