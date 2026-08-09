package com.fitness.server.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 将编排层授予的工具名解析为模型可见定义。
 * 授权决定“可用什么”，注册表决定“工具长什么样”。
 */
@Component
public class AuthorizedToolResolver {

    private final ToolContractRegistry toolContractRegistry;

    public AuthorizedToolResolver(ToolContractRegistry toolContractRegistry) {
        this.toolContractRegistry = toolContractRegistry;
    }

    public List<LlmClient.Tool> resolve(List<String> allowedToolNames) {
        if (allowedToolNames == null || allowedToolNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<LlmClient.Tool> resolvedTools = new ArrayList<>(allowedToolNames.size());
        for (String toolName : allowedToolNames) {
            resolvedTools.add(requireContract(toolName).toLlmTool());
        }
        return List.copyOf(resolvedTools);
    }

    /**
     * 验证编排层授予的工具是否属于当前 Agent 的领域与能力范围。
     */
    public void validateAuthorization(
            List<String> allowedToolNames,
            Collection<String> domainOwners,
            boolean allowDraftTools
    ) {
        if (allowedToolNames == null || allowedToolNames.isEmpty()) {
            return;
        }
        if (domainOwners == null || domainOwners.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个授权领域");
        }

        for (String toolName : allowedToolNames) {
            ToolContract contract = requireContract(toolName);
            if (!domainOwners.contains(contract.getDomainOwner())) {
                throw new IllegalArgumentException("工具不属于当前领域: " + toolName);
            }
            if (!allowDraftTools && contract.getCategory() == ToolContract.ToolCategory.DRAFT) {
                throw new IllegalArgumentException("当前领域不允许草案工具: " + toolName);
            }
        }
    }

    private ToolContract requireContract(String toolName) {
        ToolContract contract = toolContractRegistry.getContract(toolName);
        if (contract == null) {
            throw new IllegalArgumentException("授权策略引用了未注册工具: " + toolName);
        }
        return contract;
    }
}
