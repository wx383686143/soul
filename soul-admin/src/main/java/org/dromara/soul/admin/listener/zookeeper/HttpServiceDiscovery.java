/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.soul.admin.listener.zookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.admin.entity.SelectorDO;
import org.dromara.soul.admin.listener.DataChangedEvent;
import org.dromara.soul.admin.mapper.SelectorMapper;
import org.dromara.soul.admin.service.SelectorService;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.dto.convert.DivideUpstream;
import org.dromara.soul.common.enums.ConfigGroupEnum;
import org.dromara.soul.common.enums.DataEventTypeEnum;
import org.dromara.soul.common.utils.GsonUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The type Http service discovery.
 *
 * @author xiaoyu
 * @deprecated sice 2.2.0  Deprecated
 */
@Component
@SuppressWarnings("all")
@Deprecated
public class HttpServiceDiscovery implements InitializingBean {

    private static final String ROOT = "/soul/register";

    private ZkClient zkClient;

    private final SelectorService selectorService;

    private final SelectorMapper selectorMapper;

    private final ApplicationEventPublisher eventPublisher;

    private final Environment env;

    private volatile List<String> contextPathList;

    /**
     * Instantiates a new Http service discovery.
     *
     * @param selectorService the selector service
     * @param selectorMapper  the selector mapper
     * @param eventPublisher  the event publisher
     * @param env             the env
     */
    @Autowired(required = false)
    public HttpServiceDiscovery(final SelectorService selectorService,
                                final SelectorMapper selectorMapper,
                                final ApplicationEventPublisher eventPublisher,
                                final Environment env) {
        this.selectorService = selectorService;
        this.selectorMapper = selectorMapper;
        this.eventPublisher = eventPublisher;
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        Boolean register = env.getProperty("soul.http.register", Boolean.class, false);
        if (!register) {
            return;
        }
        String zookeeperUrl = env.getProperty("soul.http.zookeeperUrl", "");
        if (StringUtils.isNoneBlank(zookeeperUrl)) {
            zkClient = new ZkClient(zookeeperUrl, 5000, 2000);
            boolean exists = zkClient.exists(ROOT);
            if (!exists) {
                zkClient.createPersistent(ROOT, true);
            }
            contextPathList = zkClient.getChildren(ROOT);
            updateServerNode(contextPathList);
            zkClient.subscribeChildChanges(ROOT, (parentPath, childs) -> {
                final List<String> addSubscribePath = addSubscribePath(contextPathList, childs);
                updateServerNode(addSubscribePath);
                contextPathList = childs;
            });
        }
    }

    private void updateServerNode(final List<String> serverNodeList) {
        for (String children : serverNodeList) {
            String serverPath = buildServerPath(children);
            List<String> realPath = zkClient.getChildren(serverPath);
            List<String> collect = realPath.stream().map(r -> buildRealPath(children, r)).collect(Collectors.toList());
            updateServiceList(collect, "/" + children);
            subscribeChildChanges(serverPath);
        }
    }

    private void subscribeChildChanges(final String children) {
        zkClient.subscribeChildChanges(children, (parentPath, currentChilds) -> {
            String[] split = StringUtils.split(parentPath, "/");
            String contextPath = "/" + split[2];
            if (CollectionUtils.isEmpty(currentChilds)) {
                //表示一个都没了
                updateSelectorHandler(contextPath, null);
            } else {
                List<String> uriList = new ArrayList<>();
                for (String subNode : currentChilds) {
                    // 读取节点内容
                    String data = zkClient.readData(parentPath + "/" + subNode);
                    uriList.add(data);
                }
                updateSelectorHandler(contextPath, uriList);
            }
        });
    }

    private List<String> addSubscribePath(final List<String> alreadyChildren, final List<String> currentChildren) {
        if (CollectionUtils.isEmpty(alreadyChildren)) {
            return currentChildren;
        }
        return currentChildren.stream().filter(current -> alreadyChildren.stream().noneMatch(current::equals)).collect(Collectors.toList());
    }

    private void updateServiceList(final List<String> children, final String contextPath) {
        List<String> uriList = new ArrayList<>();
        for (String subNode : children) {
            // 读取节点内容
            String data = zkClient.readData(subNode);
            uriList.add(data);
        }
        updateSelectorHandler(contextPath, uriList);
    }

    private void updateSelectorHandler(final String contextPath, final List<String> uriList) {
        SelectorDO selector = selectorService.findByName(contextPath);
        if (Objects.nonNull(selector)) {
            SelectorData selectorData = selectorService.buildByName(contextPath);
            if (uriList == null) {
                selector.setHandle("");
                selectorData.setHandle("");
            } else {
                String handler = GsonUtils.getInstance().toJson(buildDivideUpstream(uriList));
                selector.setHandle(handler);
                selectorData.setHandle(handler);
            }
            selectorMapper.updateSelective(selector);

            //发送更新事件
            // publish change event.
            eventPublisher.publishEvent(new DataChangedEvent(ConfigGroupEnum.SELECTOR, DataEventTypeEnum.UPDATE,
                    Collections.singletonList(selectorData)));
        }
    }

    private List<DivideUpstream> buildDivideUpstream(final List<String> uriList) {
        return uriList.stream().map(uri -> {
            DivideUpstream divideUpstream = DivideUpstream.builder()
                    .upstreamHost("localhost")
                    .protocol("http://")
                    .upstreamUrl(uri)
                    .weight(50)
                    .build();
            return divideUpstream;
        }).collect(Collectors.toList());
    }

    private String buildServerPath(final String serverNode) {
        return ROOT + "/" + serverNode;
    }

    private String buildRealPath(final String serverNode, final String path) {
        return ROOT + "/" + serverNode + "/" + path;
    }
}
