<template>
  <div class="wrapper">
    <div class="details-view-tools">
      <large-button
        v-if="generateKeyVisible"
        class="button-spacing"
        outlined
        @click="generateDialog = true"
        data-test="security-server-tls-certificate-generate-key-button"
        >{{ $t('ssTlsCertificate.generateKey') }}</large-button
      >
      <file-upload
        accepts=".pem, .cer, .der"
        @fileChanged="onImportFileChanged"
        v-slot="{ upload }"
      >
        <large-button
          v-if="importCertificateVisible"
          class="button-spacing"
          outlined
          @click="upload"
          data-test="security-server-tls-certificate-import-certificate-key"
          >{{ $t('ssTlsCertificate.importCertificate') }}</large-button
        >
      </file-upload>
      <large-button
        v-if="exportCertificateVisible"
        class="button-spacing"
        outlined
        :loading="exportPending"
        @click="exportCertificate()"
        data-test="security-server-tls-certificate-export-certificate-button"
        >{{ $t('ssTlsCertificate.exportCertificate') }}</large-button
      >
    </div>

    <generate-tls-and-certificate-dialog
      :dialog="generateDialog"
      @cancel="generateDialog = false"
      @saved="newCertificateGenerated"
    />

    <div class="content-title">{{ $t('ssTlsCertificate.keyCertTitle') }}</div>
    <div class="horizontal-line-dark"></div>

    <div class="content-wrap">
      <div>
        <div class="key-wrap">
          <i class="icon-xrd_key icon"></i>
          {{ $t('ssTlsCertificate.keyText') }}
        </div>
        <div class="cert-wrap">
          <i
            class="icon-xrd_certificate icon clickable"
            @click="certificateClick()"
          ></i>
          <div
            class="clickable-link"
            v-if="certificate"
            @click="certificateClick()"
          >
            {{ certificate.hash | colonize }}
          </div>
        </div>
      </div>

      <div>
        <SmallButton
          v-if="generateCsrVisible"
          class="table-button-fix"
          @click="generateCsr()"
          data-test="security-server-tls-certificate-generate-csr-button"
          >{{ $t('ssTlsCertificate.generateCsr') }}</SmallButton
        >
      </div>
    </div>

    <div class="horizontal-line-light"></div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { Permissions, RouteName } from '@/global';
import { CertificateDetails } from '@/openapi-types';
import * as api from '@/util/api';
import LargeButton from '@/components/ui/LargeButton.vue';
import SmallButton from '@/components/ui/SmallButton.vue';
import GenerateTlsAndCertificateDialog from '@/views/KeysAndCertificates/SecurityServerTlsCertificate/GenerateTlsAndCertificateDialog.vue';
import { saveResponseAsFile } from '@/util/helpers';
import FileUpload from '@/components/ui/FileUpload.vue';
import { FileUploadResult } from '@/ui-types';

export default Vue.extend({
  components: {
    LargeButton,
    SmallButton,
    GenerateTlsAndCertificateDialog,
    FileUpload,
  },
  data() {
    return {
      certificate: undefined as CertificateDetails | undefined,
      generateDialog: false,
      exportPending: false,
    };
  },
  computed: {
    generateKeyVisible(): boolean {
      return this.$store.getters.hasPermission(
        Permissions.GENERATE_INTERNAL_SSL,
      );
    },
    importCertificateVisible(): boolean {
      return this.$store.getters.hasPermission(
        Permissions.IMPORT_INTERNAL_SSL_CERT,
      );
    },
    exportCertificateVisible(): boolean {
      return this.$store.getters.hasPermission(
        Permissions.EXPORT_INTERNAL_SSL_CERT,
      );
    },
    generateCsrVisible(): boolean {
      return this.$store.getters.hasPermission(
        Permissions.EXPORT_INTERNAL_SSL_CERT,
      );
    },
  },
  methods: {
    certificateClick(): void {
      this.$router.push({
        name: RouteName.InternalTlsCertificate,
      });
    },
    generateCsr(): void {
      this.$router.push({
        name: RouteName.GenerateInternalCSR,
      });
    },
    fetchData(): void {
      api
        .get<CertificateDetails>(`/system/certificate`)
        .then((res) => {
          this.certificate = res.data;
        })
        .catch((error) => {
          this.$store.dispatch('showError', error);
        });
    },
    newCertificateGenerated(): void {
      this.fetchData();
      this.generateDialog = false;
    },
    exportCertificate(): void {
      this.exportPending = true;
      api
        .get('/system/certificate/export', { responseType: 'blob' })
        .then((res) => saveResponseAsFile(res, 'certs.tar.gz'))
        .catch((error) => this.$store.dispatch('showError', error))
        .finally(() => (this.exportPending = false));
    },
    onImportFileChanged(result: FileUploadResult): void {
      api
        .post('/system/certificate/import', result.buffer, {
          headers: {
            'Content-Type': 'application/octet-stream',
          },
        })
        .then(() => {
          this.$store.dispatch(
            'showSuccess',
            'ssTlsCertificate.certificateImported',
          );
          this.fetchData();
        })
        .catch((error) => this.$store.dispatch('showError', error));
    },
  },
  created() {
    this.fetchData();
  },
});
</script>

<style lang="scss" scoped>
@import '../../../assets/detail-views';
.wrapper {
  margin-top: 20px;
  width: 100%;
}

.content-title {
  color: $XRoad-Black;
  font-size: 14px;
  font-weight: 500;
  margin-top: 40px;
  margin-bottom: 12px;
}

.button-spacing {
  margin-left: 20px;
}

.content-wrap {
  margin-top: 30px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.key-wrap {
  display: flex;
}

.cert-wrap {
  display: flex;
  margin-top: 20px;
  padding-left: 40px;
}

.horizontal-line-dark {
  width: 100%;
  height: 1.5px;
  border-top: 1px solid $XRoad-Grey40;
  background-color: $XRoad-Grey10;
}

.horizontal-line-light {
  width: 100%;
  height: 1px;
  background-color: $XRoad-Grey10;
}

.icon {
  margin-left: 18px;
  margin-right: 20px;
}

.clickable {
  cursor: pointer;
}

.clickable-link {
  text-decoration: underline;
  cursor: pointer;
  height: 100%;
}
</style>
