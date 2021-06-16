

<template>
<div>
  <h1 style = "margin-left:4.5%; margin-top:-10px; margin-bottom:20px;">Cancellation</h1>

    <v-row>
        <Cancellation class="video-card" v-for="(value, index) in values" v-model="values[index]" v-bind:key="index"/>
        <Cancellation class="video-card" :isNew="true" :editMode="true" v-model="newValue" @add="append"/>
    </v-row>

</div>
</template>

<script>

const axios = require('axios').default;
import Cancellation from './Cancellation.vue';

export default {
  name: 'CancellationManager',

  components: {
    Cancellation
  },

  data: () => ({
    values: [],
    newValue: {}
  }),

  async created() {
      var temp = await axios.get(axios.backend + '/cancellations')

      this.values = temp.data._embedded.cancellations;

  },

  methods:{
    append(value){
      this.values.push(value)
      this.newValue = {}
    }
  }
};
</script>

<style>
  .video-card{width:300px; margin-left:4.5%; margin-top:50px; margin-bottom:50px;}
</style>
