<template>
  <div class="history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Clock /></el-icon> 历史记录</span>
          <el-button type="danger" link @click="clearAll">
            <el-icon><Delete /></el-icon> 清空历史
          </el-button>
        </div>
      </template>
      
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column type="index" width="50" />
        <el-table-column prop="gadgetChain" label="Gadget链" width="180">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.gadgetChain }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="command" label="命令" show-overflow-tooltip />
        <el-table-column prop="payloadType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.payloadType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">
              <el-icon><View /></el-icon> 查看
            </el-button>
            <el-button type="danger" link @click="deleteRecord(row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="dialogVisible" title="Payload详情" width="60%">
      <div v-if="currentRecord">
        <p><strong>Gadget链:</strong> {{ currentRecord.gadgetChain }}</p>
        <p><strong>命令:</strong> {{ currentRecord.command }}</p>
        <p><strong>类型:</strong> {{ currentRecord.payloadType }}</p>
        <p><strong>生成时间:</strong> {{ currentRecord.createTime }}</p>
        <el-divider />
        <p><strong>Payload数据:</strong></p>
        <el-input
          :model-value="formatPayload(currentRecord.payloadData)"
          type="textarea"
          :rows="10"
          readonly
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHistory, deleteHistory } from '@/api/payload'

const records = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentRecord = ref(null)

const fetchHistory = async () => {
  loading.value = true
  try {
    const res = await getHistory()
    records.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (record) => {
  currentRecord.value = record
  dialogVisible.value = true
}

const formatPayload = (data) => {
  if (!data) return ''
  if (typeof data === 'string') return data
  // 如果是字节数组，转换为base64
  try {
    const bytes = new Uint8Array(data)
    let binary = ''
    bytes.forEach(b => binary += String.fromCharCode(b))
    return btoa(binary)
  } catch (e) {
    return JSON.stringify(data)
  }
}

const deleteRecord = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteHistory(id)
    ElMessage.success('删除成功')
    fetchHistory()
  } catch (error) {
    // 取消删除
  }
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有历史记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 批量删除
    for (const record of records.value) {
      await deleteHistory(record.id)
    }
    ElMessage.success('清空成功')
    fetchHistory()
  } catch (error) {
    // 取消
  }
}

onMounted(fetchHistory)
</script>

<style scoped>
.history-container {
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
</style>
