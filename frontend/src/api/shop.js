import http from './http'

// 店铺与类型相关接口（与后端 ShopController / ShopTypeController 对齐）
export const shopApi = {
  // 首页类型列表
  types: () => http.get('/shop-type/list'),
  // 按类型分页查询（支持城市/排序/坐标）
  ofType: (params) => http.get('/shop/of/type', { params }),
  // 按名称模糊分页查询（支持城市/排序）
  ofName: (params) => http.get('/shop/of/name', { params }),
  // 店铺详情（后续店铺详情页使用）
  detail: (id) => http.get('/shop/' + id),
  // 地图页：全部商铺（含经纬度，前端按类型/城市过滤渲染）
  mapList: () => http.get('/shop/map/list'),
  // 全部城市列表（数据驱动，与店铺数据取并集）
  cities: () => http.get('/shop/cities'),
}
