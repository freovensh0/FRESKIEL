package dev.scdeal.freskiel.corenet

import dev.scdeal.freskiel.corerpc.AbortBashCommand
import dev.scdeal.freskiel.corerpc.AbortCommand
import dev.scdeal.freskiel.corerpc.AbortRetryCommand
import dev.scdeal.freskiel.corerpc.BashCommand
import dev.scdeal.freskiel.corerpc.CloneCommand
import dev.scdeal.freskiel.corerpc.CompactCommand
import dev.scdeal.freskiel.corerpc.CycleModelCommand
import dev.scdeal.freskiel.corerpc.CycleThinkingLevelCommand
import dev.scdeal.freskiel.corerpc.ExportHtmlCommand
import dev.scdeal.freskiel.corerpc.ExtensionUiResponseCommand
import dev.scdeal.freskiel.corerpc.FollowUpCommand
import dev.scdeal.freskiel.corerpc.ForkCommand
import dev.scdeal.freskiel.corerpc.GetAvailableModelsCommand
import dev.scdeal.freskiel.corerpc.GetCommandsCommand
import dev.scdeal.freskiel.corerpc.GetEntriesCommand
import dev.scdeal.freskiel.corerpc.GetForkMessagesCommand
import dev.scdeal.freskiel.corerpc.GetLastAssistantTextCommand
import dev.scdeal.freskiel.corerpc.GetMessagesCommand
import dev.scdeal.freskiel.corerpc.GetSessionStatsCommand
import dev.scdeal.freskiel.corerpc.GetStateCommand
import dev.scdeal.freskiel.corerpc.GetTreeCommand
import dev.scdeal.freskiel.corerpc.NewSessionCommand
import dev.scdeal.freskiel.corerpc.PromptCommand
import dev.scdeal.freskiel.corerpc.RpcCommand
import dev.scdeal.freskiel.corerpc.SetAutoCompactionCommand
import dev.scdeal.freskiel.corerpc.SetAutoRetryCommand
import dev.scdeal.freskiel.corerpc.SetFollowUpModeCommand
import dev.scdeal.freskiel.corerpc.SetModelCommand
import dev.scdeal.freskiel.corerpc.SetSessionNameCommand
import dev.scdeal.freskiel.corerpc.SetSteeringModeCommand
import dev.scdeal.freskiel.corerpc.SetThinkingLevelCommand
import dev.scdeal.freskiel.corerpc.SteerCommand
import dev.scdeal.freskiel.corerpc.SwitchSessionCommand
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private typealias RpcCommandEncoder = (Json, RpcCommand) -> JsonObject

private val rpcCommandEncoders: Map<Class<out RpcCommand>, RpcCommandEncoder> =
    mapOf(
        PromptCommand::class.java to typedEncoder(PromptCommand.serializer()),
        SteerCommand::class.java to typedEncoder(SteerCommand.serializer()),
        FollowUpCommand::class.java to typedEncoder(FollowUpCommand.serializer()),
        AbortCommand::class.java to typedEncoder(AbortCommand.serializer()),
        AbortRetryCommand::class.java to typedEncoder(AbortRetryCommand.serializer()),
        GetStateCommand::class.java to typedEncoder(GetStateCommand.serializer()),
        GetMessagesCommand::class.java to typedEncoder(GetMessagesCommand.serializer()),
        GetEntriesCommand::class.java to typedEncoder(GetEntriesCommand.serializer()),
        GetTreeCommand::class.java to typedEncoder(GetTreeCommand.serializer()),
        SwitchSessionCommand::class.java to typedEncoder(SwitchSessionCommand.serializer()),
        SetSessionNameCommand::class.java to typedEncoder(SetSessionNameCommand.serializer()),
        GetForkMessagesCommand::class.java to typedEncoder(GetForkMessagesCommand.serializer()),
        ForkCommand::class.java to typedEncoder(ForkCommand.serializer()),
        CloneCommand::class.java to typedEncoder(CloneCommand.serializer()),
        ExportHtmlCommand::class.java to typedEncoder(ExportHtmlCommand.serializer()),
        CompactCommand::class.java to typedEncoder(CompactCommand.serializer()),
        CycleModelCommand::class.java to typedEncoder(CycleModelCommand.serializer()),
        CycleThinkingLevelCommand::class.java to typedEncoder(CycleThinkingLevelCommand.serializer()),
        SetThinkingLevelCommand::class.java to typedEncoder(SetThinkingLevelCommand.serializer()),
        ExtensionUiResponseCommand::class.java to typedEncoder(ExtensionUiResponseCommand.serializer()),
        NewSessionCommand::class.java to typedEncoder(NewSessionCommand.serializer()),
        GetCommandsCommand::class.java to typedEncoder(GetCommandsCommand.serializer()),
        GetLastAssistantTextCommand::class.java to typedEncoder(GetLastAssistantTextCommand.serializer()),
        BashCommand::class.java to typedEncoder(BashCommand.serializer()),
        AbortBashCommand::class.java to typedEncoder(AbortBashCommand.serializer()),
        GetSessionStatsCommand::class.java to typedEncoder(GetSessionStatsCommand.serializer()),
        GetAvailableModelsCommand::class.java to typedEncoder(GetAvailableModelsCommand.serializer()),
        SetModelCommand::class.java to typedEncoder(SetModelCommand.serializer()),
        SetAutoCompactionCommand::class.java to typedEncoder(SetAutoCompactionCommand.serializer()),
        SetAutoRetryCommand::class.java to typedEncoder(SetAutoRetryCommand.serializer()),
        SetSteeringModeCommand::class.java to typedEncoder(SetSteeringModeCommand.serializer()),
        SetFollowUpModeCommand::class.java to typedEncoder(SetFollowUpModeCommand.serializer()),
    )

fun encodeRpcCommand(
    json: Json,
    command: RpcCommand,
): JsonObject {
    val basePayload = serializeRpcCommand(json, command)

    return buildJsonObject {
        basePayload.forEach { (key, value) ->
            put(key, value)
        }

        if (!basePayload.containsKey("type")) {
            put("type", command.type)
        }

        val commandId = command.id
        if (commandId != null && !basePayload.containsKey("id")) {
            put("id", commandId)
        }
    }
}

private fun serializeRpcCommand(
    json: Json,
    command: RpcCommand,
): JsonObject {
    val encoder =
        rpcCommandEncoders[command.javaClass]
            ?: error("Unsupported RPC command type: ${command::class.qualifiedName}")

    return encoder(json, command)
}

@Suppress("UNCHECKED_CAST")
private fun <T : RpcCommand> typedEncoder(serializer: KSerializer<T>): RpcCommandEncoder {
    return { currentJson, currentCommand ->
        currentJson.encodeToJsonElement(serializer, currentCommand as T).jsonObject
    }
}
