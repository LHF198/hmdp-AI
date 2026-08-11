import http from './http'

// 代金券与秒杀订单接口（与后端 VoucherController / VoucherOrderController 对齐）
export const voucherApi = {
  // 店铺代金券列表
  listByShop: (shopId) => http.get('/voucher/list/' + shopId),
  // 我的秒杀订单列表
  orders: () => http.get('/voucher-order/list'),
  // 秒杀抢购，返回订单 id
  seckill: (voucherId) => http.post('/voucher-order/seckill/' + voucherId),
  // 模拟支付
  pay: (orderId) => http.put('/voucher-order/pay/' + orderId),
}
