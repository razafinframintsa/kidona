<template>
  <div class="xrd-tab-max-width xrd-view-common">
    <subViewTitle :title="serviceClientId" @close="close" />

    <v-card flat>
      <table class="xrd-table service-client-margin">
        <thead>
          <tr>
            <th>{{ $t('serviceClients.name') }}</th>
            <th>{{ $t('serviceClients.id') }}</th>
          </tr>
        </thead>
        <tr>
          <td>{{ serviceClient.name }}</td>
          <td>{{ serviceClient.id }}</td>
        </tr>
      </table>
    </v-card>

    <div class="group-members-row">
      <div class="row-title">{{ $t('serviceClients.accessRights') }}</div>
      <div class="row-buttons">
        <large-button
          @click="showConfirmDeleteAll = true"
          outlined
          data-test="remove-all-access-rights"
          v-if="serviceClientAccessRights.length > 0"
          >{{ $t('serviceClients.removeAll') }}
        </large-button>
        <large-button
          @click="showAddServiceDialog()"
          outlined
          data-test="add-subjects-dialog"
          >{{ $t('serviceClients.addService') }}
        </large-button>
      </div>
    </div>

    <table
      class="xrd-table service-client-margin"
      v-if="serviceClientAccessRights.length > 0"
    >
      <thead>
        <tr>
          <th>{{ $t('serviceClients.serviceCode') }}</th>
          <th>{{ $t('serviceClients.title') }}</th>
          <th>{{ $t('serviceClients.accessRightsGiven') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="accessRight in keyedServiceClientAccessRights()"
          :key="accessRight.uiKey"
        >
          <td>{{ accessRight.service_code }}</td>
          <td>{{ accessRight.service_title }}</td>
          <td>{{ accessRight.rights_given_at }}</td>
          <td class="button-wrap">
            <v-btn
              small
              outlined
              rounded
              color="primary"
              class="xrd-small-button xrd-table-button"
              data-test="access-right-remove"
              @click="remove(accessRight)"
              >{{ $t('action.remove') }}</v-btn
            >
          </td>
        </tr>
      </tbody>
    </table>

    <h3 v-else class="service-client-margin">
      {{ $t('serviceClients.noAccessRights') }}
    </h3>

    <div class="footer-buttons-wrap">
      <large-button @click="close()" data-test="close">{{
        $t('action.close')
      }}</large-button>
    </div>

    <AddServiceClientServiceDialog
      v-if="isAddServiceDialogVisible"
      :dialog="isAddServiceDialogVisible"
      :serviceCandidates="serviceCandidates()"
      @save="addService"
      @cancel="hideAddService"
    >
    </AddServiceClientServiceDialog>

    <!-- Confirm dialog delete group -->
    <confirmDialog
      :dialog="showConfirmDeleteAll"
      title="serviceClients.removeAllTitle"
      text="serviceClients.removeAllText"
      @cancel="showConfirmDeleteAll = false"
      @accept="removeAll()"
    />

    <confirmDialog
      :dialog="showConfirmDeleteOne"
      title="serviceClients.removeOneTitle"
      text="serviceClients.removeOneText"
      @cancel="resetDeletionSettings()"
      @accept="doRemoveAccessRight()"
    />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import * as api from '@/util/api';
import {
  AccessRight,
  AccessRights,
  ServiceClient,
  ServiceDescription,
} from '@/openapi-types';
import SubViewTitle from '@/components/ui/SubViewTitle.vue';
import LargeButton from '@/components/ui/LargeButton.vue';
import AddServiceClientServiceDialog from '@/views/Clients/ServiceClients/AddServiceClientServiceDialog.vue';
import { serviceCandidatesForServiceClient } from '@/util/serviceClientUtils';
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue';
import { ServiceCandidate } from '@/ui-types';
import { sortAccessRightsByServiceCode } from '@/util/sorting';

interface UiAccessRight extends AccessRight {
  uiKey: number;
}

export default Vue.extend({
  components: {
    SubViewTitle,
    LargeButton,
    AddServiceClientServiceDialog,
    ConfirmDialog,
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    serviceClientId: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      serviceClientAccessRights: [] as AccessRight[],
      serviceClient: {} as ServiceClient,
      accessRightToDelete: null as AccessRight | null,
      isAddServiceDialogVisible: false as boolean,
      clientServiceDescriptions: [] as ServiceDescription[],
      showConfirmDeleteAll: false as boolean,
      showConfirmDeleteOne: false as boolean,
    };
  },
  methods: {
    fetchData(): void {
      this.fetchAccessRights();
      this.fetchServiceDescriptions();
      api
        .get<ServiceClient>(
          `/clients/${this.id}/service-clients/${this.serviceClientId}`,
        )
        .then((response) => (this.serviceClient = response.data))
        .catch((error) => this.$store.dispatch('showError', error));
    },
    fetchServiceDescriptions(): void {
      api
        .get<ServiceDescription[]>(`/clients/${this.id}/service-descriptions`)
        .then((response) => {
          this.clientServiceDescriptions = response.data;
        })
        .catch((error) => this.$store.dispatch('showError', error));
    },
    fetchAccessRights(): void {
      api
        .get<AccessRight[]>(
          `/clients/${this.id}/service-clients/${this.serviceClientId}/access-rights`,
        )
        .then((response) => {
          this.serviceClientAccessRights = sortAccessRightsByServiceCode(
            response.data,
          );
        })
        .catch((error) => this.$store.dispatch('showError', error));
    },
    close(): void {
      this.$router.go(-1);
    },
    resetDeletionSettings(): void {
      this.showConfirmDeleteOne = false;
      this.accessRightToDelete = null;
    },
    remove(accessRight: AccessRight): void {
      this.showConfirmDeleteOne = true;
      this.accessRightToDelete = accessRight;
    },
    doRemoveAccessRight(): void {
      api
        .post(
          `/clients/${this.id}/service-clients/${this.serviceClientId}/access-rights/delete`,
          { items: [{ service_code: this.accessRightToDelete?.service_code }] },
        )
        .then(() => {
          this.$store.dispatch('showSuccess', 'serviceClients.removeSuccess');
          if (this.serviceClientAccessRights.length === 1) {
            this.serviceClientAccessRights = [];
          } else {
            this.fetchAccessRights();
          }
        })
        .catch((error) => this.$store.dispatch('showError', error))
        .finally(() => {
          this.showConfirmDeleteOne = false;
          this.accessRightToDelete = null;
        });
    },
    addService(accessRights: AccessRight[]): void {
      this.hideAddService();
      const accessRightsObject: AccessRights = { items: accessRights };
      api
        .post(
          `/clients/${this.id}/service-clients/${this.serviceClientId}/access-rights`,
          accessRightsObject,
        )
        .then(() => {
          this.$store.dispatch(
            'showSuccess',
            'serviceClients.addServiceClientAccessRightSuccess',
          );
          this.fetchAccessRights();
        })
        .catch((error) => this.$store.dispatch('showError', error));
    },
    hideAddService(): void {
      this.isAddServiceDialogVisible = false;
    },
    showAddServiceDialog(): void {
      this.isAddServiceDialogVisible = true;
    },
    removeAll(): void {
      this.showConfirmDeleteAll = false;

      api
        .post(
          `/clients/${this.id}/service-clients/${this.serviceClientId}/access-rights/delete`,
          {
            items: this.serviceClientAccessRights.map((item: AccessRight) => ({
              service_code: item.service_code,
            })),
          },
        )
        .then(() => {
          this.$store.dispatch('showSuccess', 'serviceClients.removeSuccess');
          this.serviceClientAccessRights = [];
        })
        .catch((error) => this.$store.dispatch('showError', error));
    },
    serviceCandidates(): ServiceCandidate[] {
      return serviceCandidatesForServiceClient(
        this.clientServiceDescriptions,
        this.serviceClientAccessRights,
      );
    },

    keyedServiceClientAccessRights(): UiAccessRight[] {
      return this.serviceClientAccessRights.map(
        (sca: AccessRight, index: number) => {
          return { ...sca, uiKey: index };
        },
      ) as UiAccessRight[];
    },
  },
  created(): void {
    this.fetchData();
  },
});
</script>

<style lang="scss" scoped>
@import '../../../assets/tables';
@import '../../../assets/global-style';

.group-members-row {
  width: 100%;
  display: flex;
  margin-top: 70px;
  align-items: baseline;

  .row-buttons {
    display: flex;
    * {
      margin-left: 20px;
    }
  }

  .row-title {
    width: 100%;
    justify-content: space-between;
    color: #202020;
    font-family: Roboto;
    font-size: 20px;
    font-weight: 500;
    letter-spacing: 0.5px;
  }
}

.button-wrap {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.service-client-margin {
  margin-top: 40px;
}

.footer-buttons-wrap {
  margin-top: 48px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid $XRoad-Grey40;
  padding-top: 20px;
}
</style>
