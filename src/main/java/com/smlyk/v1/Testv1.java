package com.smlyk.v1;

import com.smlyk.v1.mapper.Blog;
import com.smlyk.v1.mapper.BlogMapper;

/**
 * @author yekai
 */
public class Testv1 {


    public static void main(String[] args) {

        YKSqlSession sqlSession = new YKSqlSession(new YKConfiguration(), new YKExecutor());
        BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);

        Blog blog = mapper.selectBlogById(1);
        System.out.println(blog);
    }

}
