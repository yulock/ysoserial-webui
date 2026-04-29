<template>
  <div class="lab-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><Collection /></el-icon> 测试目标管理</span>
              <el-button type="primary" @click="openAddDialog">
                <el-icon><Plus /></el-icon> 添加目标
              </el-button>
            </div>
          </template>

          <el-empty v-if="!targets.length && !loading" description="暂无测试目标" />

          <el-card v-for="target in targets" :key="target.id" class="target-card" shadow="hover">
            <div class="target-item">
              <div class="target-info">
                <h4>{{ target.name }}</h4>
                <p><el-icon><Location /></el-icon> {{ target.host }}:{{ target.port }}</p>
                <el-tag size="small" :type="typeColor(target.type)">{{ target.type.toUpperCase() }}</el-tag>
                <p class="target-desc" v-if="target.description">{{ target.description }}</p>
              </div>
              <div class="target-actions">
                <el-button type="primary" link @click="editTarget(target)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" link @click="handleDelete(target.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </el-card>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><Info-filled /></el-icon> 使用说明</span>
            </div>
          </template>

          <div class="instructions">
            <h4>1. 添加测试目标</h4>
            <p>配置目标主机的地址、端口和类型，可用于Payload的远程测试。</p>

            <h4>2. Payload生成与测试</h4>
            <p>在Payload页面选择Gadget链，输入要执行的命令生成Payload，然后选择已配置的测试目标进行远程发送测试。</p>

            <h4>3. 测试验证</h4>
            <ul>
              <li><strong>Socket模式</strong>: 直接发送序列化字节到目标端口，并尝试读取响应。</li>
              <li><strong>URL模式</strong>: 通过HTTP POST发送Payload，适用于基于HTTP的反序列化漏洞。</li>
              <li><strong>本地验证</strong>: 仅验证Payload结构完整性，不发送到远程。</li>
            </ul>

            <h4>4. 安全警告</h4>
            <el-alert
              title="本工具仅供安全研究和教学使用"
              type="warning"
              description="请勿用于非法用途，使用本工具造成的任何后果由使用者自行承担"
              show-icon
              :closable="false"
            />

            <h4>5. 支持的Gadget链</h4>
            <ul>
              <li>CommonsCollections系列 (CC1-CC7)</li>
              <li>CommonsBeanutils1</li>
              <li>Spring系列</li>
              <li>URLDNS (DNS探测)</li>
              <li>C3P0</li>
              <li>AspectJWeaver</li>
              <li>Groovy1</li>
              <li>Jdk7u21</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showDialog" :title="isEdit ? '编辑目标' : '添加目标'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="输入目标名称" />
        </el-form-item>
        <el-form-item label="主机" prop="host">
          <el-input v-model="form.host" placeholder="127.0.0.1" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="测试类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="Socket (TCP直连)" value="socket" />
            <el-option label="URL (HTTP POST)" value="url" />
          </el-select>
        </el-form-item>
        <el-form-item label="URL地址" prop="url" v-if="form.type === 'url'">
          <el-input v-model="form.url" placeholder="http://target:8080/endpoint" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTargets, saveTarget, deleteTarget } from '@/api/lab'

const targets = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  host: '127.0.0.1',
  port: 8080,
  type: 'socket',
  url: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入目标名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入目标主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口号', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  url: [{ required: true, message: '请输入URL地址', trigger: 'blur' }]
}

const typeColor = (type) => {
  const map = { socket: 'success', url: 'primary', rmi: 'warning', jrmp: 'danger', other: 'info' }
  return map[type] || 'info'
}

const fetchTargets = async () => {
  loading.value = true
  try {
    const res = await getTargets()
    targets.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.host = '127.0.0.1'
  form.port = 8080
  form.type = 'socket'
  form.url = ''
  form.description = ''
  showDialog.value = true
  if (formRef.value) formRef.value.resetFields()
}

const editTarget = (target) => {
  isEdit.value = true
  form.id = target.id
  form.name = target.name
  form.host = target.host
  form.port = target.port
  form.type = target.type || 'socket'
  form.url = target.url || ''
  form.description = target.description || ''
  showDialog.value = true
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const data = {
      name: form.name,
      host: form.host,
      port: form.port,
      type: form.type,
      description: form.description
    }
    if (isEdit.value) {
      data.id = form.id
    }
    if (form.type === 'url') {
      data.host = form.host
      data.url = form.url
    }

    await saveTarget(data)
    ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
    showDialog.value = false
    fetchTargets()
  } catch (error) {
    console.error(error)
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个测试目标吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTarget(id)
    targets.value = targets.value.filter(t => t.id !== id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  fetchTargets()
})
</script>

<style scoped>
.lab-container {
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

.target-card {
  margin-bottom: 15px;
}

.target-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.target-info h4 {
  margin: 0 0 8px;
  color: #303133;
}

.target-info p {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.target-desc {
  color: #909399;
  font-size: 12px;
}

.instructions h4 {
  margin: 20px 0 10px;
  color: #303133;
}

.instructions h4:first-child {
  margin-top: 0;
}

.instructions p {
  margin: 5px 0;
  color: #606266;
  line-height: 1.6;
}

.instructions ul {
  margin: 10px 0;
  padding-left: 20px;
  color: #606266;
}

.instructions li {
  margin: 5px 0;
}
</style>
