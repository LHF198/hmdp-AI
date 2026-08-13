/**
 * 订单状态字典：与后端 com.hmdp.enums.OrderStatusEnum 保持一致（tb_voucher_order.status）。
 * 前后端各自维护一份，状态码变更时必须同步修改。
 */
export const ORDER_STATUS = {
  UNPAID: 1, // 未支付
  PAID: 2, // 已支付
  USED: 3, // 已核销
  CANCELLED: 4, // 已取消
  REFUNDING: 5, // 退款中
  REFUNDED: 6, // 已退款
}

/**
 * 状态码 → 展示文案
 */
export const ORDER_STATUS_TEXT = {
  [ORDER_STATUS.UNPAID]: '未支付',
  [ORDER_STATUS.PAID]: '已支付',
  [ORDER_STATUS.USED]: '已核销',
  [ORDER_STATUS.CANCELLED]: '已取消',
  [ORDER_STATUS.REFUNDING]: '退款中',
  [ORDER_STATUS.REFUNDED]: '已退款',
}
