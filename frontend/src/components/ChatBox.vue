<template>
  <v-flex @submit.prevent="sendMessage(message)">
    <v-form style="width:90%" class="ma-auto pt-3">
      <v-text-field
        ref="message"
        v-model="message"
        label="Solo"
        :placeholder="$t('chat_box.type_a_message')"
        solo
        append-icon="mdi-send"
        @click:append="sendMessage(message)"
      >
        <template v-slot:prepend-inner>
          <drawpad />
          <v-btn icon @click="activateFileUploadDialog(true)">
            <v-icon>mdi-file-plus</v-icon>
          </v-btn>
          <new-poll-dialog />
        </template>
      </v-text-field>
    </v-form>
  </v-flex>
</template>

<script>
import * as types from "@/store/types.js";
import NewPollDialog from "./NewPollDialog";
import { call } from "vuex-pathify";
import { interfaceControl } from "@/store/modules/interfaceControl";
import RegisterStoreModule from "@/mixins/RegisterStoreModule";
import Drawpad from "./Drawpad";

export default {
  components: { NewPollDialog, Drawpad },
  mixins: [RegisterStoreModule],
  data() {
    return {
      message: ""
    };
  },
  created() {
    this.registerStoreModule("interfaceControl", interfaceControl);
  },
  methods: {
    activateFileUploadDialog: call(
      `interfaceControl/${types.setFileUploadDialog}`
    ),
    sendMessage(message) {
      this.$store.dispatch(`chat/${types.sendMessage}`, { message });
      this.message = "";
    }
  }
};
</script>

<style lang="css"></style>
