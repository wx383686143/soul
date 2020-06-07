package org.dromara.soul.test.alibaba.dubbo.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.dromara.soul.client.dubbo.common.annotation.SoulDubboClient;
import org.dromara.soul.test.dubbo.api.entity.ComplexBeanTest;
import org.dromara.soul.test.dubbo.api.entity.DubboTest;
import org.dromara.soul.test.dubbo.api.service.DubboMultiParamService;
import org.springframework.stereotype.Service;

/**
 * The type Dubbo multi param service.
 */
@Service("dubboMultiParamService")
public class DubboMultiParamServiceImpl implements DubboMultiParamService {
    
    @Override
    @SoulDubboClient(path = "/findByIdsAndName", desc = "根据用户ids与名称查询")
    public DubboTest findByIdsAndName(List<Integer> ids, String name) {
        DubboTest test = new DubboTest();
        test.setId(ids.toString());
        test.setName("hello world soul alibaba dubbo param findByIdsAndName ：" + name);
        return test;
    }
    
    @Override
    @SoulDubboClient(path = "/findByArrayIdsAndName", desc = "")
    public DubboTest findByArrayIdsAndName(Integer[] ids, String name) {
        DubboTest test = new DubboTest();
        test.setId(Arrays.toString(ids));
        test.setName("hello world soul alibaba dubbo param findByArrayIdsAndName ：" + name);
        return test;
    }
    
    @Override
    @SoulDubboClient(path = "/findByStringArray", desc = "")
    public DubboTest findByStringArray(String[] ids) {
        DubboTest test = new DubboTest();
        test.setId(Arrays.toString(ids));
        test.setName("hello world soul alibaba dubbo param findByStringArray");
        return test;
    }
    
    @Override
    @SoulDubboClient(path = "/findByListId", desc = "")
    public DubboTest findByListId(List<String> ids) {
        DubboTest test = new DubboTest();
        test.setId(ids.toString());
        test.setName("hello world soul alibaba dubbo param findByListId");
        return test;
    }
    
    @Override
    @SoulDubboClient(path = "/batchSave", desc = "")
    public DubboTest batchSave(List<DubboTest> dubboTestList) {
        DubboTest test = new DubboTest();
        test.setId(dubboTestList.stream().map(DubboTest::getId).collect(Collectors.joining("-")));
        test.setName("hello world soul alibaba dubbo param batchSave :" + dubboTestList.stream().map(DubboTest::getName).collect(Collectors.joining("-")));
        return test;
    }
    
    @Override
    @SoulDubboClient(path = "/batchSaveAndNameAndId", desc = "")
    public DubboTest batchSaveAndNameAndId(List<DubboTest> dubboTestList, String id, String name) {
        DubboTest test = new DubboTest();
        test.setId(id);
        test.setName("hello world soul alibaba dubbo param batchSaveAndNameAndId :" + name + ":" + dubboTestList.stream().map(DubboTest::getName).collect(Collectors.joining("-")));
        return test;
    }
    
    @Override
    @SoulDubboClient(path = "/saveComplexBeanTest", desc = "")
    public DubboTest saveComplexBeanTest(ComplexBeanTest complexBeanTest) {
        DubboTest test = new DubboTest();
        test.setId(complexBeanTest.getIdLists().toString());
        test.setName("hello world soul alibaba dubbo param saveComplexBeanTest :" + complexBeanTest.getDubboTest().getName());
        return test;
    }
    
}
