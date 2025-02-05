<template>
  <div>
    <LargeButton
      data-test="delete-client-button"
      @click="confirmDelete = true"
      outlined
      >{{ $t('action.delete') }}</LargeButton
    >

    <!-- Confirm dialog for delete client -->
    <ConfirmDialog
      :dialog="confirmDelete"
      :loading="deleteLoading"
      title="client.action.delete.confirmTitle"
      text="client.action.delete.confirmText"
      @cancel="confirmDelete = false"
      @accept="deleteClient()"
    />

    <!-- Confirm dialog for deleting orphans -->
    <ConfirmDialog
      :dialog="confirmOrphans"
      :loading="orphansLoading"
      title="client.action.removeOrphans.confirmTitle"
      text="client.action.removeOrphans.confirmText"
      cancelButtonText="client.action.removeOrphans.cancelButtonText"
      @cancel="notDeleteOrphans()"
      @accept="deleteOrphans()"
    />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { RouteName } from '@/global';
import LargeButton from '@/components/ui/LargeButton.vue';
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue';
import * as api from '@/util/api';

export default Vue.extend({
  components: {
    LargeButton,
    ConfirmDialog,
  },
  props: {
    id: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      orphansLoading: false as boolean,
      confirmDelete: false as boolean,
      deleteLoading: false as boolean,
      confirmOrphans: false as boolean,
    };
  },

  methods: {
    deleteClient(): void {
      this.deleteLoading = true;
      api.remove(`/clients/${this.id}`).then(
        () => {
          this.$store.dispatch('showSuccess', 'client.action.delete.success');
          this.checkOrphans();
        },
        (error) => {
          this.$store.dispatch('showError', error);
          this.confirmDelete = false;
          this.deleteLoading = false;
        },
      );
    },

    checkOrphans(): void {
      api.get(`/clients/${this.id}/orphans`).then(
        () => {
          this.confirmDelete = false;
          this.deleteLoading = false;
          this.confirmOrphans = true;
        },
        (error) => {
          this.confirmDelete = false;
          this.deleteLoading = false;
          if (error.response.status === 404) {
            // No orphans found so exit the view
            this.$router.replace({ name: RouteName.Clients });
          } else {
            // There was some other error, but the client is already deleted so exit the view
            this.$store.dispatch('showError', error);
            this.$router.replace({ name: RouteName.Clients });
          }
        },
      );
    },

    deleteOrphans(): void {
      this.orphansLoading = true;
      api
        .remove(`/clients/${this.id}/orphans`)
        .then(
          () => {
            this.$store.dispatch(
              'showSuccess',
              'client.action.removeOrphans.success',
            );
          },
          (error) => {
            // There was some other error, but the client is already deleted so exit the view
            this.$store.dispatch('showError', error);
          },
        )
        .finally(() => {
          this.confirmOrphans = false;
          this.orphansLoading = false;
          this.$router.replace({ name: RouteName.Clients });
        });
    },

    notDeleteOrphans(): void {
      this.confirmOrphans = false;
      this.$router.replace({ name: RouteName.Clients });
    },
  },
});
</script>
