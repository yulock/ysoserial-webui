<template>
  <div class="payload-container">
    <el-row :gutter="20">
      <!-- 左侧：Payload生成表单 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><Magic /></el-icon> Payload生成</span>
            </div>
          </template>
          
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
          >
            <el-form-item label="Gadget链" prop="gadgetName">
              <el-select
                v-model="form.gadgetName"
                placeholder="请选择Gadget链"
                style="width: 100%"
                filterable
                @change="onGadgetChange"
              >
                <el-option
                  v-for="gadget in gadgets"
                  :key="gadget.name"
                  :label="gadget.name"
                  :value="gadget.name"
                >
                  <div class="gadget-option">
                    <span class="gadget-name">{{ gadget.name }}</span>
                    <el-tag v-if="gadget.authors" size="small" type="info">{{ gadget.authors }}</el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="selectedGadget">
              <el-alert
                :title="`依赖: ${selectedGadget.dependencies || '无'}`"
                type="info"
                :closable="false"
                show-icon
              />
            </el-form-item>
            
            <el-form-item label="执行命令" prop="command">
              <el-input
                v-model="form.command"
                type="textarea"
                :rows="3"
                placeholder="请输入要执行的命令，如: whoami"
              />
            </el-form-item>
            
            <el-form-item label="输出格式" prop="payloadType">
              <el-radio-group v-model="form.payloadType">
                <el-radio label="base64">Base64</el-radio>
                <el-radio label="hex">Hex</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="generating"
                @click="handleGenerate"
              >
                <el-icon><Magic /></el-icon> 生成Payload
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 生成进度 -->
        <el-card v-if="progressVisible" style="margin-top: 20px">
          <template #header>
            <span>生成进度</span>
          </template>
          <div class="progress-container">
            <el-timeline>
              <el-timeline-item
                v-for="(step, index) in progressSteps"
                :key="index"
                :type="step.status"
                :icon="step.icon"
              >
                {{ step.title }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：结果展示和测试 -->
      <el-col :span="12">
        <el-card v-if="result">
          <template #header>
            <div class="card-header">
              <span><el-icon><Document /></el-icon> 生成结果</span>
              <div>
                <el-tag type="success" effect="dark">大小: {{ result.size }} bytes</el-tag>
              </div>
            </div>
          </template>
          
          <el-input
            v-model="result.payload"
            type="textarea"
            :rows="10"
            readonly
          />
          
          <div class="result-actions">
            <el-button type="primary" @click="copyPayload">
              <el-icon><Copy-document /></el-icon> 复制
            </el-button>
            <el-button @click="downloadPayload">
              <el-icon><Download /></el-icon> 下载
            </el-button>
          </div>
        </el-card>
        
        <!-- 在线测试 -->
        <el-card v-if="result" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span><el-icon><Connection /></el-icon> 在线测试</span>
            </div>
          </template>
          
          <el-form :model="testForm" label-position="top">
            <el-form-item label="目标主机" v-if="testForm.type === 'socket'">
              <el-input v-model="testForm.host" placeholder="127.0.0.1" />
            </el-form-item>
            
            <el-form-item label="目标端口" v-if="testForm.type === 'socket'">
              <el-input-number v-model="testForm.port" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>

            <el-form-item label="目标URL" v-if="testForm.type === 'url'">
              <el-input v-model="testForm.url" placeholder="http://example.com/endpoint" />
            </el-form-item>
            
            <el-form-item label="测试类型">
              <el-radio-group v-model="testForm.type">
                <el-radio label="socket">Socket发送</el-radio>
                <el-radio label="url">URL发送</el-radio>
                <el-radio label="local">本地验证</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="danger"
                :loading="testing"
                @click="handleTest"
              >
                <el-icon><Promotion /></el-icon> 发送测试
              </el-button>
            </el-form-item>
          </el-form>
          
          <el-alert
            v-if="testResult"
            :title="testResult"
            :type="testSuccess ? 'success' : 'error'"
            show-icon
            :closable="false"
            style="margin-top: 15px"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getGadgets, generatePayload, testPayload } from '@/api/payload'

const formRef = ref()
const gadgets = ref([])
const generating = ref(false)
const testing = ref(false)
const result = ref(null)
const progressVisible = ref(false)
const testResult = ref('')
const testSuccess = ref(false)

const form = reactive({
  gadgetName: '',
  command: '',
  payloadType: 'base64'
})

const testForm = reactive({
  host: '127.0.0.1',
  port: 8080,
  url: '',
  type: 'socket'
})

const progressSteps = ref([
  { title: '初始化...', status: 'primary', icon: 'Loading' },
  { title: '加载Gadget链...', status: '', icon: '' },
  { title: '构建Payload对象...', status: '', icon: '' },
  { title: '序列化...', status: '', icon: '' },
  { title: '编码...', status: '', icon: '' }
])

const selectedGadget = computed(() => {
  return gadgets.value.find(g => g.name === form.gadgetName)
})

const rules = {
  gadgetName: [
    { required: true, message: '请选择Gadget链', trigger: 'change' }
  ],
  command: [
    { required: true, message: '请输入命令', trigger: 'blur' }
  ]
}

const onGadgetChange = () => {
  // Gadget选择变化时的处理
}

const handleGenerate = async () => {
  try {
    await formRef.value.validate()
    generating.value = true
    progressVisible.value = true
    
    // 模拟进度
    for (let i = 0; i < progressSteps.value.length; i++) {
      progressSteps.value[i].status = 'primary'
      progressSteps.value[i].icon = 'Loading'
      await new Promise(r => setTimeout(r, 300))
      progressSteps.value[i].status = 'success'
      progressSteps.value[i].icon = 'Check'
    }
    
    const res = await generatePayload({
      gadgetName: form.gadgetName,
      command: form.command,
      payloadType: form.payloadType
    })
    
    result.value = res.data
    ElMessage.success('生成成功')
  } catch (error) {
    console.error(error)
    progressSteps.value.forEach(step => {
      if (step.status === 'primary') {
        step.status = 'danger'
        step.icon = 'Close'
      }
    })
  } finally {
    generating.value = false
  }
}

const handleTest = async () => {
  if (!result.value) {
    ElMessage.warning('请先生成Payload')
    return
  }

  if (testForm.type === 'url' && !testForm.url) {
    ElMessage.warning('请输入目标URL')
    return
  }
  
  testing.value = true
  try {
    const res = await testPayload({
      payload: result.value.payload,
      host: testForm.host,
      port: testForm.port,
      url: testForm.url,
      type: testForm.type
    })
    testResult.value = res.data
    testSuccess.value = true
  } catch (error) {
    testResult.value = error.message || '测试失败'
    testSuccess.value = false
  } finally {
    testing.value = false
  }
}

const copyPayload = () => {
  navigator.clipboard.writeText(result.value.payload)
  ElMessage.success('已复制到剪贴板')
}

const downloadPayload = () => {
  const blob = new Blob([result.value.payload], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `payload_${form.gadgetName}_${Date.now()}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  try {
    const res = await getGadgets()
    gadgets.value = res.data || []
  } catch (error) {
    console.error(error)
  }
})
</script>

<style scoped>
.payload-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .el-icon {
  margin-right: 5px;
}

.gadget-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.gadget-name {
  font-weight: bold;
}

.result-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.progress-container {
  padding: 10px;
}
</style>
