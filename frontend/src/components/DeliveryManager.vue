

<template>
<div>
  <h1 style = "margin-left:4.5%; margin-top:-10px; margin-bottom:20px;">Delivery</h1>

    <v-row>
        <Delivery class="video-card" v-for="(value, index) in values" v-model="values[index]" v-bind:key="index"/>
        <Delivery class="video-card" :isNew="true" :editMode="true" v-model="newValue" @add="append"/>
    </v-row>

</div>
</template>

<script>

const axios = require('axios').default;
import Delivery from './Delivery.vue';

export default {
  name: 'DeliveryManager',

  components: {
    Delivery
  },

  data: () => ({
    values: [],
    newValue: {}
  }),

  async created() {
      var temp = await axios.get(axios.backend + '/deliveries')

      this.values = temp.data._embedded.deliveries;

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
