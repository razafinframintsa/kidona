<template>
  <v-container class="xrd-view-common justify-center wrapper">
    <sub-view-title
      :title="$t('apiKey.createApiKey.title')"
      :show-close="true"
      @close="close"
    ></sub-view-title>
    <v-stepper :alt-labels="true" v-model="step" class="stepper mt-2">
      <v-stepper-header class="stepper-header">
        <v-stepper-step :complete="step > 1" step="1">{{
          $t('apiKey.createApiKey.step.roles.name')
        }}</v-stepper-step>
        <v-divider />
        <v-stepper-step :complete="keyGenerated" step="2">{{
          $t('apiKey.createApiKey.step.keyDetails.name')
        }}</v-stepper-step>
      </v-stepper-header>
      <v-stepper-items>
        <v-stepper-content step="1" class="pa-0">
          <v-row class="mb-5">
            <v-col>
              <h3>{{ $t('apiKey.createApiKey.step.roles.description') }}</h3>
            </v-col>
          </v-row>
          <v-row no-gutters v-for="role in roles" :key="role">
            <v-col class="checkbox-wrapper">
              <v-checkbox
                v-model="selectedRoles"
                height="10px"
                :value="role"
                :label="$t(`apiKey.role.${role}`)"
              />
            </v-col>
          </v-row>
          <v-row class="stepper-item-footer mt-12" no-gutters>
            <v-col>
              <large-button outlined @click="close">
                {{ $t('action.cancel') }}
              </large-button>
            </v-col>
            <v-col class="text-right">
              <large-button :disabled="nextButtonDisabled" @click="step++">
                {{ $t('action.next') }}
              </large-button>
            </v-col>
          </v-row>
        </v-stepper-content>
        <v-stepper-content step="2">
          <v-row>
            <v-col class="text-right">
              <large-button
                :disabled="keyGenerated"
                :loading="generatingKey"
                @click="generateKey"
              >
                {{ $t('apiKey.createApiKey.step.keyDetails.createKeyButton') }}
              </large-button>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6" sm="3" class="api-key-label">
              {{ $t('apiKey.createApiKey.step.keyDetails.apiKey') }}
            </v-col>
            <v-col cols="6" sm="9">
              {{ apiKey.key }}
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6" sm="3" class="api-key-label">
              {{ $t('apiKey.createApiKey.step.keyDetails.apiKeyID') }}
            </v-col>
            <v-col cols="6" sm="9">
              {{ apiKey.id }}
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6" sm="3" class="api-key-label">
              {{ $t('apiKey.createApiKey.step.keyDetails.assignedRoles') }}
            </v-col>
            <v-col cols="6" sm="9">
              {{ translatedRoles | commaSeparate }}
            </v-col>
          </v-row>
          <v-row class="mt-12">
            <v-col>
              {{ $t('apiKey.createApiKey.step.keyDetails.note') }}
            </v-col>
          </v-row>
          <v-row class="stepper-item-footer mt-12" no-gutters>
            <v-col>
              <large-button
                outlined
                @click="close"
                :disabled="keyGenerated || generatingKey"
              >
                {{ $t('action.cancel') }}
              </large-button>
            </v-col>
            <v-col class="text-right">
              <large-button
                outlined
                @click="step--"
                class="mr-5"
                :disabled="keyGenerated || generatingKey"
              >
                {{ $t('action.previous') }}
              </large-button>
              <large-button :disabled="!keyGenerated" @click="close">
                {{ $t('action.finish') }}
              </large-button>
            </v-col>
          </v-row>
        </v-stepper-content>
      </v-stepper-items>
    </v-stepper>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue';
import LargeButton from '@/components/ui/LargeButton.vue';
import SubViewTitle from '@/components/ui/SubViewTitle.vue';
import { Roles } from '@/global';
import { ApiKey } from '@/global-types';
import * as api from '@/util/api';

export default Vue.extend({
  name: 'CreateApiKeyStepper',
  components: {
    LargeButton,
    SubViewTitle,
  },
  data() {
    return {
      step: 1,
      roles: Roles,
      generatingKey: false,
      selectedRoles: [] as string[],
      apiKey: {} as ApiKey,
    };
  },
  computed: {
    nextButtonDisabled(): boolean {
      return this.selectedRoles.length === 0;
    },
    translatedRoles(): string[] {
      return !this.apiKey.roles
        ? []
        : this.apiKey.roles.map(
            (role) => this.$t(`apiKey.role.${role}`) as string,
          );
    },
    keyGenerated(): boolean {
      return this.apiKey.key !== undefined;
    },
  },
  methods: {
    close(): void {
      this.$router.back();
    },
    async generateKey() {
      this.generatingKey = true;
      api
        .post<ApiKey>('/api-keys', this.selectedRoles)
        .then((resp) => {
          this.apiKey = resp.data;
          this.$store.dispatch('showSuccess', 'apiKey.createApiKey.success');
        })
        .catch((error) => this.$store.dispatch('showError', error))
        .finally(() => (this.generatingKey = false));
    },
  },
});
</script>

<style scoped lang="scss">
@import '../../../assets/detail-views';
@import '../../../assets/colors';
.wrapper {
  max-width: 850px;
  height: 100%;
  width: 100%;
  color: $XRoad-Grey60;
}
.stepper {
  box-shadow: unset;
}
.stepper-header {
  box-shadow: unset;
  width: 50%;
  margin: auto;
}
.stepper-item-footer {
  margin-top: 20px;
  padding-top: 30px;
  border-top: 1px solid $XRoad-Grey40;
}
.checkbox-wrapper {
  border-bottom: solid 1px $XRoad-Grey10;
}
.api-key-label {
  font-weight: 500;
}
h3 {
  color: $XRoad-Grey60;
  font-weight: 400;
}
</style>
