package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author Liyanbao
 * @create 2020-04-27 20:33
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {

}
