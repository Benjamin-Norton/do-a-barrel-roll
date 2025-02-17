package nl.enjarai.doabarrelroll.net;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;

public interface SyncableConfig<SELF extends SyncableConfig<SELF>> {
    Integer getSyncTimeout();

    Text getSyncTimeoutMessage();

    Codec<? super SELF> getTransferCodec();
}
