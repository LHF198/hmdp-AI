<template>
  <div class="ai-page-root">
    <div class="ai-page-inner">
      <div class="ai-page-card">
        <header class="ai-chat-header">
          <div class="ai-chat-header-left">
            <div class="ai-brand">
              <el-icon :size="20"><Service /></el-icon>
            </div>
            <div class="ai-header-title">
              <span>AI探店助手</span>
              <span>AI探店助手陪您一起解锁探店新思路</span>
            </div>
          </div>
          <div class="ai-chat-header-actions">
            <button
              class="ai-icon-button"
              @click="startNewConversation"
              :disabled="isLoading"
              title="开启新对话"
              aria-label="开启新对话"
            >
              <el-icon :size="16"><Plus /></el-icon>
            </button>
            <button
              class="ai-close-button"
              @click="handleClose"
              title="返回上一页"
              aria-label="关闭并返回"
            >
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>
        </header>

        <main class="ai-chat-body" ref="chatContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="ai-message-row"
            :class="{ 'ai-right': message.role === 'user' }"
          >
            <div class="ai-avatar">
              <el-icon :size="18">
                <User v-if="message.role === 'user'" /><Service v-else />
              </el-icon>
            </div>
            <div class="ai-message-bubble">
              <div v-if="message.role === 'assistant' && message.isLoading" class="ai-typing-dots">
                <span></span><span></span><span></span>
              </div>
              <div v-else>{{ message.content }}</div>
              <span v-if="message.isStreaming" style="margin-left: 2px">|</span>
            </div>
          </div>
        </main>

        <footer class="ai-chat-footer">
          <textarea
            ref="textarea"
            v-model="userInput"
            class="ai-input"
            rows="1"
            placeholder="请问我有什么可以帮助您的嘛..."
            @keydown.enter.exact.prevent="sendMessage"
            @input="adjustTextareaHeight"
          ></textarea>
          <button
            class="ai-send-button"
            @click="isLoading ? stopResponse() : sendMessage()"
            :disabled="!isLoading && !userInput.trim()"
            :title="isLoading ? '停止回答' : '发送'"
            aria-label="发送消息"
          >
            <el-icon :size="20">
              <SwitchButton v-if="isLoading" /><Promotion v-else />
            </el-icon>
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
// AI 探店助手：SSE 纯文本流逐字渲染（GET /api/ai/chat/stream）、AbortController 停止回答、
// 开启新对话（重置会话 ID + 清空消息）；路由卸载时自动中断流（旧 MPA 页面 aiassistant.html 的 SPA 迁移版）
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const messages = ref([])
const userInput = ref('')
const isLoading = ref(false)
const sessionId = ref(Date.now().toString())
const controller = ref(null)
const chatContainer = ref(null)
const textarea = ref(null)

// 消息变化自动滚动到底部
watch(
  messages,
  () => {
    scrollToBottom()
  },
  { deep: true }
)

onMounted(() => {
  startNewConversation()
  nextTick(focusTextarea)
})

onBeforeUnmount(() => {
  // SPA 路由切换不会触发 beforeunload，必须在卸载时中断流式请求
  stopResponse()
})

function handleClose() {
  if (isLoading.value) {
    stopResponse()
  }
  // SPA 内优先回退到来源页；无历史记录时回首页
  router.back()
  setTimeout(() => {
    if (window.location.pathname.endsWith('/ai')) {
      router.replace('/')
    }
  }, 300)
}

function startNewConversation() {
  stopResponse()
  messages.value = []
  sessionId.value = Date.now().toString()
  messages.value.push({
    role: 'assistant',
    content: '你好! 我是您的专属AI探店助手,请问有什么能帮到您？',
    isLoading: false,
    isStreaming: false,
  })
  nextTick(scrollToBottom)
  nextTick(focusTextarea)
}

function adjustTextareaHeight() {
  const el = textarea.value
  if (!el) {
    return
  }
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

function scrollToBottom() {
  nextTick(() => {
    const container = chatContainer.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

function focusTextarea() {
  nextTick(() => {
    const el = textarea.value
    if (el) {
      el.focus()
      adjustTextareaHeight()
    }
  })
}

function sendMessage() {
  if (!userInput.value.trim() || isLoading.value) {
    return
  }
  _sendMessage()
}

async function _sendMessage() {
  if (controller.value) {
    controller.value.abort()
  }
  controller.value = typeof AbortController !== 'undefined' ? new AbortController() : null

  const userMessage = {
    role: 'user',
    content: userInput.value.trim(),
    isLoading: false,
    isStreaming: false,
  }
  messages.value.push(userMessage)

  const assistantMessage = {
    role: 'assistant',
    content: '',
    isLoading: true,
    isStreaming: false,
  }
  messages.value.push(assistantMessage)

  const question = userInput.value.trim()
  userInput.value = ''
  adjustTextareaHeight()
  scrollToBottom()
  isLoading.value = true

  try {
    const options = { method: 'GET', headers: {} }
    // 添加 token 到请求头
    const token = sessionStorage.getItem('token')
    if (token) {
      options.headers['authorization'] = token
    }
    if (controller.value && controller.value.signal) {
      options.signal = controller.value.signal
    }

    const response = await fetch(
      '/api/ai/chat/stream?message=' +
        encodeURIComponent(question) +
        '&conversationId=' +
        sessionId.value,
      options
    )
    if (!response.ok) {
      throw new Error('HTTP error! status: ' + response.status)
    }

    const messageIndex = messages.value.length - 1
    const reader = response.body && response.body.getReader ? response.body.getReader() : null

    if (!reader) {
      const text = await response.text()
      // 检查是否是错误响应
      try {
        const jsonResp = JSON.parse(text)
        if (jsonResp.success === false && jsonResp.errorMsg) {
          messages.value[messageIndex].content = '抱歉，AI服务暂时不可用：' + jsonResp.errorMsg
        } else {
          messages.value[messageIndex].content = text
        }
      } catch (e) {
        messages.value[messageIndex].content = text
      }
    } else {
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      messages.value[messageIndex].isLoading = false
      messages.value[messageIndex].isStreaming = true

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          messages.value[messageIndex].isStreaming = false
          // 检查完整响应是否是错误 JSON
          if (buffer.startsWith('{"success":false')) {
            try {
              const jsonErr = JSON.parse(buffer)
              if (jsonErr.errorMsg) {
                messages.value[messageIndex].content = '抱歉，AI服务暂时不可用：' + jsonErr.errorMsg
              }
            } catch (e) {
              // 保持原内容
            }
          }
          break
        }
        buffer += decoder.decode(value, { stream: true })
        // 如果检测到错误 JSON，停止流式显示
        if (buffer.startsWith('{"success":false')) {
          messages.value[messageIndex].isStreaming = false
          try {
            const jsonErr = JSON.parse(buffer)
            if (jsonErr.errorMsg) {
              messages.value[messageIndex].content = '抱歉，AI服务暂时不可用：' + jsonErr.errorMsg
            }
          } catch (e) {
            // 保持原内容
          }
          break
        }
        messages.value[messageIndex].content = buffer
        scrollToBottom()
      }
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('请求出错:', error)
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage) {
        lastMessage.content = '抱歉，请求过程中出现错误: ' + error.message
        lastMessage.isLoading = false
        lastMessage.isStreaming = false
      }
    }
  } finally {
    isLoading.value = false
    if (messages.value.length) {
      const finalMessage = messages.value[messages.value.length - 1]
      finalMessage.isLoading = false
      finalMessage.isStreaming = false
    }
    controller.value = null
    scrollToBottom()
  }
}

function stopResponse() {
  if (controller.value) {
    controller.value.abort()
  }
  controller.value = null
  isLoading.value = false
  if (messages.value.length) {
    const lastMessage = messages.value[messages.value.length - 1]
    lastMessage.isLoading = false
    lastMessage.isStreaming = false
  }
}
</script>
