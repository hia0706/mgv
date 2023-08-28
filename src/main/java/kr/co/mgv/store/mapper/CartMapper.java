package kr.co.mgv.store.mapper;

import kr.co.mgv.store.vo.Cart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CartMapper {
    void insertCart(Cart cart);

    List<Cart> getCartItemsByUserId(String userId);

    Cart updateCart(Cart cart);
}
