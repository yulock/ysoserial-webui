<template>
  <div class="lab-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><Collection /></el-icon> 测试目标管理</span>
              <el-button type="primary" @click="showAddDialog = true">
                <el-icon><Plus /></el-icon> 添加目标
              </el-button>
            </div>
          </template>
          
          <el-empty v-if="!targets.length" description="暂无测试目标" />
          
          <el-card v-for="target in targets" :key="target.id" class="target-card" shadow="hover">
            <div class="target-item">
              <div class="target-info">
                <h4>{{ target.name }}</h4>
                <p><el-icon><Location /></el-icon> {{ target.host }}:{{ target.port }}</p>
                <p class="target-desc">{{ target.description || '无描述' }}</p>
              </div>
              <div class="target-actions">
                <el-button type="primary" link @click="editTarget(target)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" link @click="deleteTarget(target.id)">
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
            <h4>1. Payload生成</h4>
            <p>选择Gadget链，输入要执行的命令，点击生成按钮即可生成对应的反序列化Payload。</p>
            
            <h4>2. 在线测试</h4>
            <p>生成Payload后，可以配置测试目标，将Payload发送到目标端口进行测试。</p>
            
            <h4>3. 安全警告</h4>
            <el-alert
              title="本工具仅供安全研究和教学使用"
              type="warning"
              description="请勿用于非法用途，使用本工具造成的任何后果由使用者自行承担"
              show-icon
              :closable="false"
            />
            
            <h4>4. 支持的Gadget链</h4>
            <ul>
              <li>CommonsCollections系列 (CC1-CC7)</li>
              <li>CommonsBeanutils1</li>
              <li>Spring系列</li>
              <li>URLDNS (DNS探测)</li>
              <li>C3P0</li>
              <li>AspectJWeaver</li>
              <li>ROME</li>
              <li>Groovy1</li>
              <li>Jdk7u21</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑目标' : '添加目标'" width="500px">
      <el-form :model="targetForm" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="targetForm.name" placeholder="输入目标名称" />
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="targetForm.host" placeholder="127.0.0.1" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="targetForm.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="targetForm.type" style="width: 100%">
            <el-option label="RMI" value="rmi" />
            <el-option label="JRMP" value="jrmp" />
            <el-option label="JMX" value="jmx" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="targetForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTarget">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const targets = ref([])
const showAddDialog = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const targetForm = reactive({
  name: '',
  host: '127.0.0.1',
  port: 8080,
  type: 'other',
  description: ''
})

const editTarget = (target) => {
  isEdit.value = true
  currentId.value = target.id
  Object.assign(targetForm, target)
  showAddDialog.value = true
}

const saveTarget = () => {
  if (isEdit.value) {
    const index = targets.value.findIndex(t => t.id === currentId.value)
    if (index !== -1) {
      targets.value[index] = { ...targetForm, id: currentId.value }
    }
  } else {
    targets.value.push({
      ...targetForm,
      id: Date.now()
    })
  }
  showAddDialog.value = false
  resetForm()
  ElMessage.success('保存成功')
}

const deleteTarget = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个目标吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    targets.value = targets.value.filter(t => t.id !== id)
    ElMessage.success('删除成功')
  } catch (error) {
    // 取消
  }
}

const resetForm = () => {
  isEdit.value = false
  currentId.value = null
  targetForm.name = ''
  targetForm.host = '127.0.0.1'
  targetForm.port = 8080
  targetForm.type = 'other'
  targetForm.description = ''
}
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
