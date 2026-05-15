package com.dipilodopilasaurus.leashablecollars.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.UUID;

public record OwnerData(UUID uuid, String name, Optional<UUID> owned, Optional<String> ownedName) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<OwnerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUID_CODEC.fieldOf("uuid").forGetter(OwnerData::uuid),
            Codec.STRING.fieldOf("name").forGetter(OwnerData::name),
        UUID_CODEC.optionalFieldOf("owned").forGetter(OwnerData::owned),
            Codec.STRING.optionalFieldOf("owned_name").forGetter(OwnerData::ownedName)
    ).apply(instance, OwnerData::new));

    public OwnerData(UUID uuid, String name) {
        this(uuid, name, Optional.empty(), Optional.empty());
    }

    public OwnerData withOwned(UUID ownedUuid, String ownedName) {
        return new OwnerData(this.uuid, this.name, Optional.of(ownedUuid), Optional.of(ownedName));
    }

    public OwnerData clearOwned() {
        return new OwnerData(this.uuid, this.name);
    }
}